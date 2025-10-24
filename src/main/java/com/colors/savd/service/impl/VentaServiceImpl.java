package com.colors.savd.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final CanalVentaRepository canalRepo; // crea este repo si no lo tienes aún
  private final TemporadaRepository temporadaRepo; // idem
  private final KardexRepository kardexRepo;

  @Override
  @Transactional
  public Long registrarVentaManual(VentaManualDTO dto, Long usuarioId) {
    // Validaciones mínimas
    if (dto.getItems() == null || dto.getItems().isEmpty())
      throw new BusinessException("La venta debe tener al menos 1 ítem.");

    // Regla: si canal = FISICO → referencia_origen obligatoria
    var canal = canalRepo.findById(dto.getCanalId())
        .orElseThrow(() -> new BusinessException("Canal no encontrado"));

    if ("FISICO".equalsIgnoreCase(canal.getCodigo()) && (dto.getReferenciaOrigen() == null || dto.getReferenciaOrigen().isBlank()))
      throw new BusinessException("Referencia de venta es obligatoria para canal FÍSICO.");

    Venta v = new Venta();
    v.setFechaHora(dto.getFechaHora() != null ? dto.getFechaHora() : LocalDateTime.now());
    v.setCanal(canal);
    if (dto.getTemporadaId() != null) {
      v.setTemporada(temporadaRepo.findById(dto.getTemporadaId())
          .orElseThrow(() -> new BusinessException("Temporada no encontrada")));
    }
    v.setReferenciaOrigen(dto.getReferenciaOrigen());
    v.setEstado(EstadoVenta.ACTIVA);
    v.setTotal(BigDecimal.ZERO);
    v.setCreatedAt(LocalDateTime.now());
    v.setUpdatedAt(LocalDateTime.now());
    v.setCreatedBy(Usuario.builder().id(usuarioId).build());

    v = ventaRepo.save(v);

    BigDecimal total = BigDecimal.ZERO;

    for (var it : dto.getItems()) {
      VarianteSku sku = skuRepo.findById(it.getSkuId())
          .orElseThrow(() -> new BusinessException("SKU no encontrado: " + it.getSkuId()));

      var det = new VentaDetalle();
      det.setVenta(v);
      det.setSku(sku);
      det.setCantidad(it.getCantidad());
      det.setPrecioUnitario(it.getPrecioUnitario());
      det.setPrecioLista(sku.getPrecioLista());
      det.setImporte(it.getPrecioUnitario().multiply(BigDecimal.valueOf(it.getCantidad())));
      ventaDetRepo.save(det);

      total = total.add(det.getImporte());

      // Registrar movimiento en Kardex (VENTA, signo -1)
      KardexMovimiento k = new KardexMovimiento();
      k.setFechaHora(v.getFechaHora());
      // TODO: setear tipo_mov_id=VENTA (cargar TipoMovimiento por código)
      k.setSku(sku);
      k.setCantidad(it.getCantidad());
      k.setSigno(-1);
      k.setCanal(canal);
      k.setVenta(v);
      k.setVentaDetalle(det);
      k.setReferencia(v.getReferenciaOrigen());
      k.setUsuario(Usuario.builder().id(usuarioId).build());
      k.setCreatedAt(LocalDateTime.now());
      kardexRepo.save(k);
    }

    v.setTotal(total);
    v.setUpdatedAt(LocalDateTime.now());
    ventaRepo.save(v);

    return v.getId();
  }

  @Override
  @Transactional
  public void anularVenta(Long ventaId, Long usuarioId, String motivo) {
    Venta v = ventaRepo.findById(ventaId)
        .orElseThrow(() -> new BusinessException("Venta no existe"));
    if (v.getEstado() == EstadoVenta.ANULADA) return;

    // Regla: invertir kardex de la venta (crear movimientos ANULACION con signo +1)
    var detalles = ventaDetRepo.findByVenta_Id(ventaId);
    for (VentaDetalle det : detalles) {
      KardexMovimiento k = new KardexMovimiento();
      k.setFechaHora(LocalDateTime.now());
      // TODO: tipo_mov = ANULACION (cargar TipoMovimiento por código)
      k.setSku(det.getSku());
      k.setCantidad(det.getCantidad());
      k.setSigno(+1);
      k.setCanal(v.getCanal());
      k.setVenta(v);
      k.setVentaDetalle(det);
      k.setReferencia("ANUL-" + v.getReferenciaOrigen());
      k.setObservacion(motivo);
      k.setUsuario(Usuario.builder().id(usuarioId).build());
      k.setCreatedAt(LocalDateTime.now());
      kardexRepo.save(k);
    }

    v.setEstado(com.colors.savd.model.enums.EstadoVenta.ANULADA);
    v.setAnuladaAt(LocalDateTime.now());
    v.setAnuladaPor(Usuario.builder().id(usuarioId).build());
    v.setUpdatedAt(LocalDateTime.now());
    ventaRepo.save(v);
  }
}
