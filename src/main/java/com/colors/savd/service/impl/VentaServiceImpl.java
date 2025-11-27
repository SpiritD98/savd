package com.colors.savd.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colors.savd.dto.LineaVentaDTO;
import com.colors.savd.dto.VentaManualDTO;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.model.*;
import com.colors.savd.model.enums.EstadoVenta;
import com.colors.savd.repository.*;
import com.colors.savd.service.VentaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor 
@Slf4j
public class VentaServiceImpl implements VentaService {

  private final VentaRepository ventaRepo;
  private final VentaDetalleRepository ventaDetRepo;
  private final VarianteSkuRepository skuRepo;
  private final CanalVentaRepository canalRepo;
  private final TemporadaRepository temporadaRepo; 
  private final KardexRepository kardexRepo;
  private final TipoMovimientoRepository tipoMovRepo;
  private final UsuarioRepository usuarioRepo;

  @Override
  @Transactional
  public Long registrarVentaManual(VentaManualDTO dto, Long usuarioId) {
    // Validaciones mínimas
    if (dto.getItems() == null || dto.getItems().isEmpty()){
      throw new BusinessException("La venta debe tener al menos 1 ítem.");
    }

    // 1) Cargar canal
    CanalVenta canal = canalRepo.findById(dto.getCanalId())
        .orElseThrow(() -> new BusinessException("Canal no encontrado"));

    // 2) Normalizar referencia y rega de canal FISICO
    String referencia = StringUtils.trimToNull(dto.getReferenciaOrigen());
    if ("FISICO".equalsIgnoreCase(canal.getCodigo()) && referencia == null) {
      throw new BusinessException("Referencia de venta es obligatoria para canal FÍSICO.");
    }

    // 3) Fecha efectiva (normalizada a segundos para consistencia / duplicidad)
    LocalDateTime fechaEf = (dto.getFechaHora() != null ? dto.getFechaHora() : LocalDateTime.now()).withNano(0);

    // 4) Chequeo de duplicidad por cabecera (si hay referencia)
    if (referencia != null && Boolean.TRUE.equals(ventaRepo.existsByFechaHoraAndCanal_IdAndReferenciaOrigen(fechaEf, canal.getId(), referencia))) {
      throw new BusinessException("Ya existe una venta con esa fecha/canal/referencia.");
    }

    // 5) Temporada: fija (si viene) o automatica (si no viene)
    Temporada temporada = null;
    if (dto.getTemporadaId() != null) {
      temporada = temporadaRepo.findById(dto.getTemporadaId())
      .orElseThrow(() -> new BusinessException("Temporada no encontrada"));
    }else{
      temporada = temporadaRepo.findActivaQueContenga(fechaEf.toLocalDate()).orElse(null); //puede ser null
    }

    // 6) Tipo de movimiento VENTA
    TipoMovimiento tipoVenta = tipoMovRepo.findByCodigo("VENTA")
    .orElseThrow(() -> new BusinessException("Tipomovimiento 'VENTA' no configurado"));

    // 7) Usuario responsable
    Usuario userRef = usuarioRepo.findById(usuarioId)
    .orElseThrow(() -> new BusinessException("Usuario no encontrado id= " + usuarioId));
    
    // === 8) PRECHECK DE STOCK (antes de persistir nada) ===
    // 8.1 Cargar SKUs y validar líneas (cantidad/precio)
    Map<Long, VarianteSku> skuMap = new HashMap<>();
    Set<Long> skuIdsSolicitados = new HashSet<>();
    for (LineaVentaDTO it : dto.getItems()) {
      VarianteSku sku = skuRepo.findById(it.getSkuId()).orElseThrow(() -> new BusinessException("SKU no encontrado: " + it.getSkuId()));
      if (it.getCantidad() == null || it.getCantidad() <= 0) {
        throw new BusinessException("Cantidad inválida para SKU " + sku.getSku());
      }
      if (it.getPrecioUnitario() == null || it.getPrecioUnitario().signum() < 0) {
        throw new BusinessException("Precio unitario inválido para SKU " + sku.getSku());
      }
      skuMap.put(sku.getId(), sku);
      skuIdsSolicitados.add(sku.getId());
    }

    // 8.2 Consultar stock disponible hasta la fecha de la venta (o ahora)
    List<Object[]> stockRows = kardexRepo.stockPorSkuHasta(skuIdsSolicitados, fechaEf);
    Map<Long, Long> stockMap = new HashMap<>();
    for (Object[] r : stockRows) {
      Long skuId = ((Number) r[0]).longValue();
      Long stock = (r[1] == null) ? 0L : ((Number) r[1]).longValue();
      stockMap.put(skuId, stock);
    }

    // 8.3 Verificar stock suficiente por línea
    for (LineaVentaDTO it : dto.getItems()) {
      long disp = stockMap.getOrDefault(it.getSkuId(), 0L);
      if (disp < it.getCantidad()) {
        VarianteSku sku = skuMap.get(it.getSkuId());
        String skuTxt = (sku != null ? sku.getSku() : String.valueOf(it.getSkuId()));
        throw new BusinessException("Stock insuficiente para SKU " + skuTxt +
            " (disponible=" + disp + ", requerido=" + it.getCantidad() + ")");
      }
    }

    // === 9) Crear Cabecera (recién aquí persisto) ===
    Venta v = new Venta();
    v.setFechaHora(fechaEf);
    v.setCanal(canal);
    v.setTemporada(temporada);
    v.setReferenciaOrigen(referencia);
    v.setEstado(EstadoVenta.ACTIVA);
    v.setTotal(BigDecimal.ZERO);
    v.setCreatedAt(LocalDateTime.now());
    v.setUpdatedAt(LocalDateTime.now());
    v.setCreatedBy(userRef);

    v = ventaRepo.save(v);

    // 10) Iterar items
    BigDecimal total = BigDecimal.ZERO;
    for (LineaVentaDTO it : dto.getItems()) {
      VarianteSku sku = skuMap.get(it.getSkuId()); // reutilizamos el cargado en precheck

      BigDecimal precioLista = (it.getPrecioLista() != null) ? it.getPrecioLista() : sku.getPrecioLista();

      // Detalle
      VentaDetalle det = new VentaDetalle();
      det.setVenta(v);
      det.setSku(sku);
      det.setCantidad(it.getCantidad());
      det.setPrecioUnitario(it.getPrecioUnitario());
      det.setPrecioLista(precioLista);

      BigDecimal importe = it.getPrecioUnitario()
          .multiply(BigDecimal.valueOf(it.getCantidad()))
          .setScale(2, RoundingMode.HALF_UP);
      det.setImporte(importe);
      ventaDetRepo.save(det);

      total = total.add(importe);

      // Kardex (VENTA: signo -1) con idempotencia
      KardexMovimiento k = new KardexMovimiento();
      k.setFechaHora(v.getFechaHora());
      k.setTipo(tipoVenta);
      k.setSku(sku);
      k.setCantidad(it.getCantidad());
      k.setSigno(-1);
      k.setCanal(canal);
      k.setVenta(v);
      k.setVentaDetalle(det);
      k.setReferencia(v.getReferenciaOrigen());
      k.setObservacion("Venta manual");
      k.setUsuario(userRef);
      k.setCreatedAt(LocalDateTime.now());
      k.setIdempotencyKey(String.format("VENTA|%d|%d", v.getId(), det.getId()));

      try {
        kardexRepo.save(k);
      } catch (DataIntegrityViolationException ex) {
        log.warn("Conflicto de idempotencia en Kardex para venta={} det={}", v.getId(), det.getId());
      }
    }

    // 11) Finalizar cabecera
    v.setTotal(total.setScale(2, RoundingMode.HALF_UP));
    v.setUpdatedAt(LocalDateTime.now());
    ventaRepo.save(v);

    return v.getId();
  }

  @Override
  @Transactional
  public void anularVenta(Long ventaId, Long usuarioId, String motivo) {
    Venta v = ventaRepo.findById(ventaId)
        .orElseThrow(() -> new BusinessException("Venta no existe"));

    if (v.getEstado() == EstadoVenta.ANULADA){ 
      return; 
    }

    TipoMovimiento tipoAnul = tipoMovRepo.findByCodigo("ANULACION")
    .orElseThrow(() -> new BusinessException("TipoMovimiento 'ANULACION' no configurado"));

    Usuario userRef = usuarioRepo.findById(usuarioId)
    .orElseThrow(() -> new BusinessException("Usuario no encontrado id=" + usuarioId));

    String motivoEf = StringUtils.defaultIfBlank(motivo, "Anulacion solicitada");

    // Regla: invertir kardex de la venta (crear movimientos ANULACION con signo +1)
    List<VentaDetalle> detalles = ventaDetRepo.findByVenta_Id(ventaId);
    for (VentaDetalle det : detalles) {
      KardexMovimiento k = new KardexMovimiento();
      k.setFechaHora(v.getFechaHora());
      k.setTipo(tipoAnul);
      k.setSku(det.getSku());
      k.setCantidad(det.getCantidad());
      k.setSigno(+1);
      k.setCanal(v.getCanal());
      k.setVenta(v);
      k.setVentaDetalle(det);
      k.setReferencia("ANUL-" + StringUtils.defaultIfBlank(v.getReferenciaOrigen(), v.getId().toString()));
      k.setObservacion(motivoEf);
      k.setUsuario(userRef);
      k.setCreatedAt(LocalDateTime.now());
      k.setIdempotencyKey(String.format("ANUL|%d|%d", v.getId(), det.getId()));

      try {
        kardexRepo.save(k);
      } catch (Exception e) {
        log.warn("Conflicto de idempotencia en Kardex (anulación) para venta={} det={}", v.getId(), det.getId());
      }
    }

    // Marcar Cabecera
    v.setEstado(EstadoVenta.ANULADA);
    v.setAnuladaAt(LocalDateTime.now());
    v.setAnuladaPor(userRef);
    v.setUpdatedAt(LocalDateTime.now());
    ventaRepo.save(v);
  }
}
