package com.colors.savd.service;

import com.colors.savd.dto.LineaVentaDTO;
import com.colors.savd.dto.VentaManualDTO;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.model.*;
import com.colors.savd.repository.*;
import com.colors.savd.service.impl.VentaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepo;
    @Mock
    private VentaDetalleRepository ventaDetRepo;
    @Mock
    private VarianteSkuRepository skuRepo;
    @Mock
    private CanalVentaRepository canalRepo;
    @Mock
    private TemporadaRepository temporadaRepo;
    @Mock
    private KardexRepository kardexRepo;
    @Mock
    private TipoMovimientoRepository tipoMovRepo;
    @Mock
    private UsuarioRepository usuarioRepo;

    @InjectMocks
    private VentaServiceImpl ventaService;

    @Test
    void registrarVentaManual_deberiaCrearVentaConUnItem() {
        // ---------- Arrange ----------
        Long usuarioId = 1L;
        Long canalId = 10L;
        Long skuId = 100L;

        LocalDateTime fecha = LocalDateTime.of(2025, 1, 1, 10, 0);

        // DTO de entrada
        LineaVentaDTO linea = LineaVentaDTO.builder()
                .skuId(skuId)
                .cantidad(2)
                .precioUnitario(new BigDecimal("50.00"))
                .build();

        VentaManualDTO dto = VentaManualDTO.builder()
                .fechaHora(fecha)
                .canalId(canalId)
                .referenciaOrigen("TICKET-001")
                .items(List.of(linea))
                .build();

        // Canal
        CanalVenta canal = new CanalVenta();
        canal.setId(canalId);
        canal.setCodigo("FISICO");
        when(canalRepo.findById(canalId)).thenReturn(Optional.of(canal));

        // No existe venta duplicada
        when(ventaRepo.existsByFechaHoraAndCanal_IdAndReferenciaOrigen(
                any(), eq(canalId), eq("TICKET-001"))
        ).thenReturn(false);

        // TipoMovimiento VENTA
        TipoMovimiento tipoVenta = new TipoMovimiento();
        tipoVenta.setId(5L);
        tipoVenta.setCodigo("VENTA");
        when(tipoMovRepo.findByCodigo("VENTA")).thenReturn(Optional.of(tipoVenta));

        // Usuario
        Usuario user = new Usuario();
        user.setId(usuarioId);
        user.setNombre("Test User");
        when(usuarioRepo.findById(usuarioId)).thenReturn(Optional.of(user));

        // SKU
        VarianteSku sku = new VarianteSku();
        sku.setId(skuId);
        sku.setSku("SKU-TEST");
        sku.setPrecioLista(new BigDecimal("60.00"));
        when(skuRepo.findById(skuId)).thenReturn(Optional.of(sku));

        // Venta: simulamos que al guardar se asigna ID
        when(ventaRepo.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta v = invocation.getArgument(0);
            if (v.getId() == null) {
                v.setId(999L);
            }
            return v;
        });

        // Detalle y kardex: solo confirmamos que se llaman
        when(ventaDetRepo.save(any(VentaDetalle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(kardexRepo.save(any(KardexMovimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ---------- Act ----------
        Long ventaId = ventaService.registrarVentaManual(dto, usuarioId);

        // ---------- Assert ----------
        assertNotNull(ventaId);
        assertEquals(999L, ventaId);

        // Verificar que se guardó la venta al menos 2 veces (cabecera + actualización total)
        verify(ventaRepo, atLeast(2)).save(any(Venta.class));

        // Verificar que se guardó el detalle
        verify(ventaDetRepo, times(1)).save(any(VentaDetalle.class));

        // Verificar que se registró movimiento en kardex
        verify(kardexRepo, times(1)).save(any(KardexMovimiento.class));

        // Verificar que no haya interacciones inesperadas
        verifyNoMoreInteractions(ventaDetRepo, kardexRepo);
    }

    @Test
    void registrarVentaManual_sinItems_deberiaLanzarBusinessException() {
        VentaManualDTO dto = VentaManualDTO.builder()
                .canalId(1L)
                .items(List.of()) // vacía
                .build();

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> ventaService.registrarVentaManual(dto, 1L)
        );

        assertTrue(ex.getMessage().contains("al menos 1 ítem"));
    }
}

