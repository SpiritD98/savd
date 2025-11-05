/*package com.colors.savd.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colors.savd.dto.VentaManualDTO;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.model.*;
import com.colors.savd.repository.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

  @Mock VentaRepository ventaRepo;
  @Mock VentaDetalleRepository ventaDetRepo;
  @Mock VarianteSkuRepository skuRepo;
  @Mock CanalVentaRepository canalRepo;
  @Mock TemporadaRepository temporadaRepo;
  @Mock KardexRepository kardexRepo;
  @Mock TipoMovimientoRepository tipoMovRepo;
  @Mock UsuarioRepository usuarioRepo;

  @InjectMocks VentaServiceImpl service;

  @Test
  void registrarVentaManual_canalFisicoSinReferencia_lanzaExcepcion() {
    // arrange
    var dto = VentaManualDTO.builder()
        .fechaHora(LocalDateTime.now())
        .canalId(1L)
        .items(List.of(VentaManualDTO.Item.builder()
            .skuId(10L).cantidad(1).precioUnitario(new BigDecimal("50")).build()))
        .build();

    when(canalRepo.findById(1L)).thenReturn(Optional.of(canalFisico("FISICO")));
    // act + assert
    assertThrows(BusinessException.class, () -> service.registrarVentaManual(dto, 99L));
  }

  @Test
  void registrarVentaManual_ok_generaKardexYTotaliza() {
    var dto = VentaManualDTO.builder()
        .fechaHora(LocalDateTime.now())
        .canalId(2L)
        .referenciaOrigen("TCK-001")
        .items(List.of(VentaManualDTO.Item.builder()
            .skuId(10L).cantidad(2).precioUnitario(new BigDecimal("30")).build()))
        .build();

    when(canalRepo.findById(2L)).thenReturn(Optional.of(canalFisico("ONLINE")));
    when(tipoMovRepo.findByCodigo("VENTA")).thenReturn(Optional.of(tipoMov("VENTA",-1)));
    when(usuarioRepo.getReferenceById(99L)).thenReturn(userRef(99L));
    when(skuRepo.findById(10L)).thenReturn(Optional.of(varSku(10L, new BigDecimal("35"))));
    when(ventaRepo.save(any())).thenAnswer(inv -> { Venta v = inv.getArgument(0); v.setId(100L); return v; });

    Long id = service.registrarVentaManual(dto, 99L);

    assertNotNull(id);
    verify(ventaDetRepo, times(1)).save(any(VentaDetalle.class));
    verify(kardexRepo, times(1)).save(any(KardexMovimiento.class));
    verify(ventaRepo, times(2)).save(any(Venta.class)); // cabecera inicial + actualización total
  }

  // helpers:
  private CanalVenta canalFisico(String codigo){ CanalVenta c=new CanalVenta(); c.setId(1L); c.setCodigo(codigo); return c; }
  private TipoMovimiento tipoMov(String cod, int signo){ var t=new TipoMovimiento(); t.setId(5L); t.setCodigo(cod); t.setSignoDefault(signo); return t; }
  private Usuario userRef(Long id){ var u=new Usuario(); u.setId(id); return u; }
  private VarianteSku varSku(Long id, BigDecimal lista){ var v=new VarianteSku(); v.setId(id); v.setPrecioLista(lista); v.setSku("SKU-"+id); return v; }
}

*/