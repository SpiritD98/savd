package com.colors.savd.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colors.savd.dto.ImportOpcionesDTO;
import com.colors.savd.dto.ImportResultadoDTO;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.model.*;
import com.colors.savd.model.enums.*;
import com.colors.savd.repository.*;
import com.colors.savd.service.ImportacionService;
import com.colors.savd.util.ExcelUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/*
 * Importa ventas desde Excel agrupando por cabecera ().
 * Columnas esperadas
 * 0: FechaHora, 1: CanalCodigo (FISICO/ONLINE), 2: Referencia, 3: SKU, 4: Cantidad, 5: PrecioUnitario, 6: PrecioLista (opcional)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportacionServiceImpl implements ImportacionService {

  private final BitacoraCargaRepository bitacoraRepo;
  private final BitacoraErrorRepository bitErrorRepo;
  private final VarianteSkuRepository skuRepo;
  private final VentaRepository ventaRepo;
  private final VentaDetalleRepository ventaDetRepo;
  private final KardexRepository kardexRepo;
  private final CanalVentaRepository canalRepo;
  private final TipoMovimientoRepository tipoMovRepo;
  private final UsuarioRepository usuarioRepo;
  private final TemporadaRepository temporadaRepo;
  private final ExcelUtil excelUtil;

  // ====== Tipos y helpers internos ======

  /** Clave de agrupación de cabecera */
  private record CabKey(LocalDateTime fechaHora, Long canalId, String referencia) { }

  /** Ítem parseado de la fila del Excel */
  private static class ParsedItem {
    int filaExcel;               // para log/bitácora
    Long skuId;
    String skuStr;
    int cantidad;
    BigDecimal precioUnit;
    BigDecimal precioLista;      // si no viene, se usará el de VarianteSku
  }

  @Override
  @Transactional
  public ImportResultadoDTO importarVentasExcel(InputStream in, String nombreArchivo, Long usuarioId, ImportOpcionesDTO opciones) {
    // === GUARD DE ROL + SOLO VALIDAR ===
    final boolean isDryRun = (opciones != null && opciones.isSoloValidar());

    // ==== Guard de seguridad: ANALISTA solo puede ejecutar en modo soloValidar ====
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean isAnalista = auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ANALISTA".equals(a.getAuthority()));

    //Analista no puede persistir (solo pre-validacion)
    if (isAnalista && !isDryRun) {
      throw new AccessDeniedException("Analista solo puede pre-validar (soloValidar=true).");
    }

    int ok = 0, err = 0;
    List<String> erroresMuestra = new ArrayList<>();
    
    //Cache para evitar consultas repetidas
    Map<String, CanalVenta> canalCache = new HashMap<>();
    Map<String, VarianteSku> skuCache = new HashMap<>();

    // Cabecera: (fechaHora, canal, referencia) → lista de items
    // Estructura de agrupación para la rama persistente
    Map<CabKey, List<ParsedItem>> grupos = new LinkedHashMap<>();

    // ==== 1) Crear bitácora si no es dry-run ====
    BitacoraCarga bit = null;
    if (!isDryRun) {
      bit = new BitacoraCarga();
      bit.setFechaHora(LocalDateTime.now());
      bit.setUsuario(usuarioRepo.findById(usuarioId).orElseThrow(() -> new BusinessException("Usuario no encontrado id=" + usuarioId)));
      bit.setTipoCarga(TipoCarga.VENTAS);
      bit.setArchivoNombre(nombreArchivo);
      bit.setFilasOk(0);
      bit.setFilasError(0);
      bit = bitacoraRepo.save(bit);
    }

    // ==== 2) Parsear Excel a estructuras de agrupación ====
    try (Workbook wb = WorkbookFactory.create(in)) {
      Sheet sheet = wb.getSheetAt(0);

      // === detectar encabezados dinamicamente ===
      Map<String, List<String>> aliases = ExcelUtil.defaultAliases();
      // Consideramos obligatorios en encabezado (Referencia puede venir vacia pero la columna idealmente existe):
      // Si deseas permitir archivos sin columna Referencia, elimina "Referencia" de requiredKeys.
      Set<String> requiredKeys = new LinkedHashSet<>(Arrays.asList("FechaHora", "CanalCodigo", "SKU", "Cantidad", "PrecioUnitario"));
      ExcelUtil.HeaderMapping hm = excelUtil.detectarEncabezados(sheet, aliases, requiredKeys, 50);

      int startRow = hm.getHeaderRowIndex() + 1;
      for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        try {
          // --- Leer por nombre de columnas ---
          LocalDateTime fechaHora = excelUtil.leerFechaHora(getCell(row, hm.col("FechaHora")));
          if (fechaHora != null) {
            fechaHora = fechaHora.withNano(0); // normalizar a segundos
          }

          String canalCodigo = excelUtil.leerString(getCell(row, hm.col("CanalCodigo")));
          //"Referencia" puede no estar en el encabezado si no es obligatorio
          Integer colRef = hm.col("Referencia");
          String referencia = (colRef != null) ? excelUtil.leerString(getCell(row, colRef)) : null;
          String skuStr           = excelUtil.leerString(getCell(row, hm.col("SKU")));
          Integer cantidad        = excelUtil.leerEntero(getCell(row, hm.col("Cantidad")));
          BigDecimal precioUnit   = excelUtil.leerDecimal(getCell(row, hm.col("PrecioUnitario")));
          Integer colPL = hm.col("PrecioLista");
          BigDecimal precioLista  = (colPL != null) ? excelUtil.leerDecimal(getCell(row, colPL)) : null;

          if (fechaHora == null || StringUtils.isBlank(canalCodigo) ||
              StringUtils.isBlank(skuStr) || cantidad == null || cantidad <= 0 ||
              precioUnit == null || precioUnit.signum() < 0) {
            throw new IllegalArgumentException("Datos obligatorios faltantes o inválidos.");
          }

          // Resolver canal por código (cacheado)
          CanalVenta canal = canalCache.computeIfAbsent(canalCodigo.toUpperCase(Locale.ROOT), code ->
              canalRepo.findByCodigo(code).orElseThrow(() ->
                  new BusinessException("Canal no configurado: " + code))
          );

          // Regla: referencia obligatoria si FISICO
          if ("FISICO".equalsIgnoreCase(canal.getCodigo()) && StringUtils.isBlank(referencia)) {
            throw new IllegalArgumentException("Referencia obligatoria para canal FISICO.");
          }

          // Resolver SKU por cadena (cacheado)
          VarianteSku sku = skuCache.computeIfAbsent(skuStr, key ->
              skuRepo.findBySku(key).orElseThrow(() ->
                  new BusinessException("SKU no encontrado: " + key))
          );

          // Armar item
          ParsedItem item = new ParsedItem();
          item.filaExcel = i + 1;
          item.skuId = sku.getId();
          item.skuStr = skuStr;
          item.cantidad = cantidad;
          item.precioUnit = precioUnit;
          item.precioLista = (precioLista != null ? precioLista : sku.getPrecioLista());

          // Si es persistente, acumula; si es dry-run, no se necesita agrupar, solo contamos ok/err
          if (!isDryRun) {
            CabKey key = new CabKey(fechaHora, canal.getId(), referencia);
            grupos.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
          }
          
          ok++; // fila válida
        } catch (Exception e) {
          err++;
          String msg = "Fila " + (i + 1) + ": " + e.getMessage();
          erroresMuestra.add(msg);

          // Solo en modo persistente registramos bitacora_error
          if (!isDryRun && bit != null) {
            BitacoraError be = new BitacoraError();
            be.setBitacora(bit);
            be.setFilaOrigen(i + 1);
            be.setCampo("GENERAL");
            be.setMensajeError(e.getMessage());
            be.setFechaHoraRegistro(LocalDateTime.now());
            bitErrorRepo.save(be);
          }

          log.error("Error importando fila {}: {}", i + 1, e.getMessage(), e);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error leyendo Excel: " + e.getMessage(), e);
    }

    // ==== 3) Persistir grupos como ventas con detalles + kardex ====
    // === Rama persistente: crear ventas/detalles/kardex + cerrar bitácora ===
    if (!isDryRun) {
      // TipoMovimiento VENTA (debe existir en catálogo)
      TipoMovimiento tipoVenta = tipoMovRepo.findByCodigo("VENTA")
          .orElseThrow(() -> new BusinessException("TipoMovimiento 'VENTA' no configurado"));
      Usuario userRef = usuarioRepo.findById(usuarioId).orElseThrow(() -> new BusinessException("Usuario no encontrado id=" + usuarioId));
      // Observacion que pondremos en kardex 
      final String observacionBase = construirObservacionBase(nombreArchivo, opciones);

      for (Map.Entry<CabKey, List<ParsedItem>> entry : grupos.entrySet()) {
        CabKey cab = entry.getKey();
        List<ParsedItem> items = entry.getValue();

        // Canal por id (desde cache no tenemos instancia; recupéralo una vez)
        CanalVenta canal = canalRepo.findById(cab.canalId()).orElseThrow(() -> new BusinessException("Canal no encontrado id=" + cab.canalId()));

        String ref = cab.referencia();
        LocalDateTime fechaCab = cab.fechaHora();

        // Evitar duplicados de cabecera (fecha+canal+ref)
        boolean dup = (ref != null && ventaRepo.existsByFechaHoraAndCanal_IdAndReferenciaOrigen(fechaCab, cab.canalId(), ref));
        if (dup) {
          log.warn("Cabecera duplicada: fecha={}, canalId={}, ref={}", fechaCab, cab.canalId(), ref);
          continue; // saltar venta completa
        }

        // ===== Asignacion de temporada segun opciones =====
        Temporada temporadaAsignada = resolvTemporadaPara(fechaCab, opciones);

        // Crear cabecera
        Venta v = new Venta();
        v.setFechaHora(fechaCab);
        v.setCanal(canal);
        v.setReferenciaOrigen(ref);
        v.setTemporada(temporadaAsignada);
        v.setEstado(EstadoVenta.ACTIVA);
        v.setTotal(BigDecimal.ZERO);
        v.setCreatedAt(LocalDateTime.now());
        v.setUpdatedAt(LocalDateTime.now());
        v.setCreatedBy(userRef);
        v = ventaRepo.save(v);

        BigDecimal total = BigDecimal.ZERO;

        for (ParsedItem it : items) {
          VarianteSku sku = skuRepo.findById(it.skuId)
              .orElseThrow(() -> new BusinessException("SKU desapareció durante import: id=" + it.skuId));

          // Detalle
          VentaDetalle det = new VentaDetalle();
          det.setVenta(v);
          det.setSku(sku);
          det.setCantidad(it.cantidad);
          det.setPrecioUnitario(it.precioUnit);
          det.setPrecioLista(it.precioLista != null ? it.precioLista : sku.getPrecioLista());

          BigDecimal importe = it.precioUnit.multiply(BigDecimal.valueOf(it.cantidad)).setScale(2, RoundingMode.HALF_UP);
          det.setImporte(importe);
          ventaDetRepo.save(det);

          total = total.add(importe);

          // Kardex (VENTA: -1) con idempotencia
          KardexMovimiento k = new KardexMovimiento();
          k.setFechaHora(v.getFechaHora());
          k.setTipo(tipoVenta);
          k.setSku(sku);
          k.setCantidad(it.cantidad);
          k.setSigno(-1);
          k.setCanal(canal);
          k.setVenta(v);
          k.setVentaDetalle(det);
          k.setReferencia(v.getReferenciaOrigen());
          k.setUsuario(userRef);
          k.setCreatedAt(LocalDateTime.now());
          k.setObservacion(observacionBase);
          k.setIdempotencyKey(generarIdemKey(v.getId(), det.getId(), "VENTA"));
          try {
            kardexRepo.save(k);
          } catch (DataIntegrityViolationException ex) {
            log.warn("Conflicto de idempotencia en Kardex (import) venta={} det={}", v.getId(), det.getId());
          }
        }

        v.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        v.setUpdatedAt(LocalDateTime.now());
        ventaRepo.save(v);
      }

      // ==== 4) Cerrar bitácora con los conteos reales ====
      bit.setFilasOk(ok);
      bit.setFilasError(err);
      bitacoraRepo.save(bit);

      return ImportResultadoDTO.builder()
        .bitacoraId(bit.getId())
        .filasOk(ok)
        .filasError(err)
        .erroresMuestra(erroresMuestra.size() > 10 ? erroresMuestra.subList(0, 10) : erroresMuestra)
        .build();
    }

    // ==== Rama dry-run: NO se persiste nada, bitácoraId = null ===
    return ImportResultadoDTO.builder()
      .bitacoraId(null)
      .filasOk(ok)
      .filasError(err)
      .erroresMuestra(erroresMuestra.size() > 10 ? erroresMuestra.subList(0, 10) : erroresMuestra)
      .build();
  }

  // === Helpers ===
  private Cell getCell(Row row, Integer colIndex){
    if (row == null || colIndex == null) return null;
    return row.getCell(colIndex);
  }

  private Temporada resolvTemporadaPara(LocalDateTime fechaHora, ImportOpcionesDTO opciones){
    if (opciones == null || opciones.getModoTemporada() == null) {
      return temporadaRepo.findActivaQueContenga(fechaHora.toLocalDate()).orElse(null);
    }
    AsignacionTemporadaModo modo = opciones.getModoTemporada();
    return switch (modo){
      case AUTOMATICA -> temporadaRepo.findActivaQueContenga(fechaHora.toLocalDate()).orElse(null);
      case FIJAR -> {
        if (opciones.getTemporadaId() == null) {
          throw new BusinessException("Se requiere temporadaId cuando el modo es FIJAR.");
        }
        Temporada t = temporadaRepo.findActivaById(opciones.getTemporadaId()).orElseThrow(() -> new BusinessException("Temporada fija no encontrada o no activa."));
        yield t;
      }
      case NINGUNA -> null;
    };
  }

  private String construirObservacionBase(String nombreArchivo, ImportOpcionesDTO opciones){
    StringBuilder sb = new StringBuilder("Importacion Excel: ").append(nombreArchivo);
    if (opciones != null && StringUtils.isNotBlank(opciones.getObservacionGeneral())) {
      sb.append(" | ").append(opciones.getObservacionGeneral().trim());
    }
    return sb.toString();
  }

  private String generarIdemKey(Long ventaId, Long detId, String tipo) {
    String base = ventaId + "|" + detId + "|" + tipo;
    return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
  }
}