package com.colors.savd.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colors.savd.dto.ImportResultadoDTO;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.model.*;
import com.colors.savd.model.enums.TipoCarga;
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
  public ImportResultadoDTO importarVentasExcel(InputStream in, String nombreArchivo, Long usuarioId) {
    // ==== 1) Crear bitácora ====
    BitacoraCarga bit = new BitacoraCarga();
    bit.setFechaHora(LocalDateTime.now());
    bit.setUsuario(usuarioRepo.getReferenceById(usuarioId));
    bit.setTipoCarga(TipoCarga.VENTAS);
    bit.setArchivoNombre(nombreArchivo);
    bit.setFilasOk(0);
    bit.setFilasError(0);
    bit = bitacoraRepo.save(bit);

    int ok = 0, err = 0;
    List<String> erroresMuestra = new ArrayList<>();

    // ==== caches para evitar consultas repetidas ====
    Map<String, CanalVenta> canalCache = new HashMap<>();
    Map<String, VarianteSku> skuCache = new HashMap<>();

    // ==== 2) Parsear Excel a estructuras de agrupación ====
    // Cabecera: (fechaHora, canal, referencia) → lista de items
    Map<CabKey, List<ParsedItem>> grupos = new LinkedHashMap<>();

    try (Workbook wb = WorkbookFactory.create(in)) {
      Sheet sheet = wb.getSheetAt(0);
      Row header = sheet.getRow(0);
      if (header == null) {
        throw new IllegalArgumentException("El archivo no contiene encabezados.");
      }

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        try {
          // --- Leer columnas obligatorias ---
          LocalDateTime fechaHora = excelUtil.leerFechaHora(row.getCell(0));
          String canalCodigo      = excelUtil.leerString(row.getCell(1));
          String referencia       = excelUtil.leerString(row.getCell(2));
          String skuStr           = excelUtil.leerString(row.getCell(3));
          Integer cantidad        = excelUtil.leerEntero(row.getCell(4));
          BigDecimal precioUnit   = excelUtil.leerDecimal(row.getCell(5));
          BigDecimal precioLista  = excelUtil.leerDecimal(row.getCell(6)); // opcional

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
          item.skuId = sku.getId();
          sku.getSku();
          item.cantidad = cantidad;
          item.precioUnit = precioUnit;
          item.precioLista = (precioLista != null ? precioLista : sku.getPrecioLista());

          // Agrupar por cabecera
          CabKey key = new CabKey(fechaHora, canal.getId(), referencia);
          grupos.computeIfAbsent(key, k -> new ArrayList<>()).add(item);

          ok++; // fila válida
        } catch (Exception e) {
          err++;
          String msg = "Fila " + (i + 1) + ": " + e.getMessage();
          erroresMuestra.add(msg);

          BitacoraError be = new BitacoraError();
          be.setBitacora(bit);
          be.setFilaOrigen(i + 1);
          be.setCampo("GENERAL");
          be.setMensajeError(e.getMessage());
          be.setFechaHoraRegistro(LocalDateTime.now());
          bitErrorRepo.save(be);

          log.error("Error importando fila {}: {}", i + 1, e.getMessage(), e);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error leyendo Excel: " + e.getMessage(), e);
    }

    // ==== 3) Persistir grupos como ventas con detalles + kardex ====
    // TipoMovimiento VENTA (debe existir en catálogo)
    TipoMovimiento tipoVenta = tipoMovRepo.findByCodigo("VENTA")
        .orElseThrow(() -> new BusinessException("TipoMovimiento 'VENTA' no configurado"));
    Usuario userRef = usuarioRepo.getReferenceById(usuarioId);

    for (Map.Entry<CabKey, List<ParsedItem>> entry : grupos.entrySet()) {
      CabKey cab = entry.getKey();
      List<ParsedItem> items = entry.getValue();

      // Canal por id (desde cache no tenemos instancia; recupéralo una vez)
      CanalVenta canal = canalRepo.findById(cab.canalId())
          .orElseThrow(() -> new BusinessException("Canal no encontrado id=" + cab.canalId()));

      // Evitar duplicados de cabecera (fecha+canal+ref)
      String ref = cab.referencia();
      boolean dup = (ref != null && ventaRepo.existsByFechaHoraAndCanal_IdAndReferenciaOrigen(cab.fechaHora(), cab.canalId(), ref));
      if (dup) {
        log.warn("Cabecera duplicada: fecha={}, canalId={}, ref={}", cab.fechaHora(), cab.canalId(), ref);
        continue; // saltar venta completa
      }

      // Crear cabecera
      Venta v = new Venta();
      v.setFechaHora(cab.fechaHora());
      v.setCanal(canal);
      v.setReferenciaOrigen(ref);
      v.setEstado(com.colors.savd.model.enums.EstadoVenta.ACTIVA);
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
        det.setImporte(it.precioUnit.multiply(BigDecimal.valueOf(it.cantidad)));
        ventaDetRepo.save(det);

        total = total.add(det.getImporte());

        // Kardex (VENTA: -1)
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
        // idempotency: hash reproducible por (ventaId, detId, "VENTA")
        k.setIdempotencyKey(generarIdemKey(v.getId(), det.getId(), "VENTA"));
        kardexRepo.save(k);
      }

      v.setTotal(total);
      v.setUpdatedAt(LocalDateTime.now());
      ventaRepo.save(v);
    }

    // ==== 4) Cerrar bitácora ====
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

  private String generarIdemKey(Long ventaId, Long detId, String tipo) {
    String base = ventaId + "|" + detId + "|" + tipo;
    return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
  }
}