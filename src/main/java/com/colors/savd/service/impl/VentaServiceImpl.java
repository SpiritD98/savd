package com.colors.savd.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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

@Service
@RequiredArgsConstructor
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

    // 3) Fecha efectiva
    LocalDateTime fechaEf = (dto.getFechaHora() != null) ? dto.getFechaHora() : LocalDateTime.now();

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
    Usuario userRef = usuarioRepo.getReferenceById(usuarioId);
    
    // 8) Crear Cabecera de Venta
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

    // 9) Iterar items
    BigDecimal total = BigDecimal.ZERO;

    for (LineaVentaDTO it : dto.getItems()) {
    VarianteSku sku = skuRepo.findById(it.getSkuId())
        .orElseThrow(() -> new BusinessException("SKU no encontrado: " + it.getSkuId()));

    if (it.getCantidad() == null || it.getCantidad() <= 0) {
      throw new BusinessException("Cantidad inválida para SKU " + sku.getSku());
    }
    if (it.getPrecioUnitario() == null || it.getPrecioUnitario().signum() < 0) {
      throw new BusinessException("Precio unitario inválido para SKU " + sku.getSku());
    }

    // Precio lista por línea (si vino), si no, el del SKU
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

    // Kardex (VENTA: signo -1)
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
    kardexRepo.save(k);
  }

    // 10) Finalizar cabecera
    v.setTotal(total.setScale(2, RoundingMode.HALF_UP));
    v.setUpdatedAt(LocalDateTime.now());
    ventaRepo.save(v);

    return v.getId();
  }

  @Override
  @Transactional
  public void anularVenta(Long ventaId, Long usuarioId, String motivo) {
    var v = ventaRepo.findById(ventaId)
        .orElseThrow(() -> new BusinessException("Venta no existe"));

    if (v.getEstado() == EstadoVenta.ANULADA){ 
      return; 
    }

    TipoMovimiento tipoAnul = tipoMovRepo.findByCodigo("ANULACION")
    .orElseThrow(() -> new BusinessException("TipoMovimiento 'ANULACION' no configurado"));

    Usuario userRef = usuarioRepo.getReferenceById(usuarioId);

    // Regla: invertir kardex de la venta (crear movimientos ANULACION con signo +1)
    List<VentaDetalle> detalles = ventaDetRepo.findByVenta_Id(ventaId);
    for (VentaDetalle det : detalles) {
      KardexMovimiento k = new KardexMovimiento();
      k.setFechaHora(LocalDateTime.now());
      k.setTipo(tipoAnul);
      k.setSku(det.getSku());
      k.setCantidad(det.getCantidad());
      k.setSigno(+1);
      k.setCanal(v.getCanal());
      k.setVenta(v);
      k.setVentaDetalle(det);
      k.setReferencia("ANUL-" + (StringUtils.defaultIfBlank(v.getReferenciaOrigen(), v.getId().toString())));
      k.setObservacion(motivo);
      k.setUsuario(userRef);
      k.setCreatedAt(LocalDateTime.now());
      k.setIdempotencyKey(String.format("ANUL|%d|%d", v.getId(), det.getId()));
      kardexRepo.save(k);
    }

    // Marcar Cabecera
    v.setEstado(EstadoVenta.ANULADA);
    v.setAnuladaAt(LocalDateTime.now());
    v.setAnuladaPor(userRef);
    v.setUpdatedAt(LocalDateTime.now());
    ventaRepo.save(v);
  }
}
