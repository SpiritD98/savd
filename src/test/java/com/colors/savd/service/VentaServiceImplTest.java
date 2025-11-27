package com.colors.savd.service;

import com.colors.savd.dto.LineaVentaDTO;
import com.colors.savd.dto.VentaManualDTO;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.model.CanalVenta;
import com.colors.savd.model.TipoMovimiento;
import com.colors.savd.model.Usuario;
import com.colors.savd.model.VarianteSku;
import com.colors.savd.model.enums.EstadoVenta;
import com.colors.savd.repository.*;
import com.colors.savd.service.impl.VentaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock private VentaRepository ventaRepo;
    @Mock private VentaDetalleRepository ventaDetRepo;
    @Mock private VarianteSkuRepository skuRepo;
    @Mock private CanalVentaRepository canalRepo;
    @Mock private TemporadaRepository temporadaRepo;
    @Mock private KardexRepository kardexRepo;
    @Mock private TipoMovimientoRepository tipoMovRepo;
    @Mock private UsuarioRepository usuarioRepo;

    @InjectMocks
    private VentaServiceImpl service;

    private CanalVenta canalOnline;
    private VarianteSku sku1;
    private TipoMovimiento tipoVenta;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        canalOnline = new CanalVenta();
        canalOnline.setId(10L);
        canalOnline.setCodigo("ONLINE");

        sku1 = new VarianteSku();
        sku1.setId(100L);
        sku1.setSku("SKU-100");
        sku1.setPrecioLista(new BigDecimal("50.00"));

        tipoVenta = new TipoMovimiento();
        tipoVenta.setId(5L);
        tipoVenta.setCodigo("VENTA");

        usuario = new Usuario();
        usuario.setId(7L);
    }

    private VentaManualDTO buildVentaDTO(long canalId, Long temporadaId, String referencia, int cantidad, BigDecimal precioUnit) {
        LineaVentaDTO item = new LineaVentaDTO();
        item.setSkuId(100L);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(precioUnit);

        VentaManualDTO dto = new VentaManualDTO();
        dto.setCanalId(canalId);
        dto.setTemporadaId(temporadaId);
        dto.setReferenciaOrigen(referencia);
        dto.setFechaHora(LocalDateTime.of(2025, 1, 10, 12, 0));
        dto.setItems(List.of(item));
        return dto;
    }

    @Test
    @DisplayName("registrarVentaManual: OK cuando stock es suficiente (persiste cabecera, detalle y kardex)")
    void registrarVentaManual_ok_conStock() {
        // Arrange
        VentaManualDTO dto = buildVentaDTO(10L, null, "REF-123", 3, new BigDecimal("20.00"));

        given(canalRepo.findById(10L)).willReturn(Optional.of(canalOnline));
        given(ventaRepo.existsByFechaHoraAndCanal_IdAndReferenciaOrigen(any(), eq(10L), eq("REF-123"))).willReturn(false);
        given(temporadaRepo.findActivaQueContenga(any(LocalDate.class))).willReturn(Optional.empty());
        given(tipoMovRepo.findByCodigo("VENTA")).willReturn(Optional.of(tipoVenta));
        given(usuarioRepo.findById(7L)).willReturn(Optional.of(usuario));
        given(skuRepo.findById(100L)).willReturn(Optional.of(sku1));

        // stock disponible = 10 (suficiente para cantidad 3)
        List<Object[]> stockRows = new ArrayList<>();
        stockRows.add(new Object[]{100L, 10L});
        given(kardexRepo.stockPorSkuHasta(anyCollection(), any(LocalDateTime.class))).willReturn(stockRows);

        // Persistencias
        willAnswer(inv -> {
            var v = (com.colors.savd.model.Venta) inv.getArgument(0);
            v.setId(123L);
            v.setEstado(EstadoVenta.ACTIVA);
            return v;
        }).given(ventaRepo).save(any());

        willAnswer(inv -> {
            var det = (com.colors.savd.model.VentaDetalle) inv.getArgument(0);
            det.setId(321L);
            return det;
        }).given(ventaDetRepo).save(any());

        // Act
        Long id = service.registrarVentaManual(dto, 7L);

        // Assert
        assertNotNull(id);
        assertEquals(123L, id);

        // Verificaciones de veces exactas
        then(ventaRepo).should(times(2)).save(any());     // crear + actualizar total
        then(ventaDetRepo).should(times(1)).save(any());  // 1 línea en el DTO
        then(kardexRepo).should(times(1)).save(any());    // kardex por la línea
        then(kardexRepo).should().stockPorSkuHasta(anyCollection(), any(LocalDateTime.class)); // se checó stock
    }

    @Test
    @DisplayName("registrarVentaManual: lanza BusinessException cuando stock es insuficiente (no persiste nada)")
    void registrarVentaManual_stockInsuficiente() {
        // Arrange: cantidad 20, pero stock reportará 5
        VentaManualDTO dto = buildVentaDTO(10L, null, "REF-456", 20, new BigDecimal("20.00"));

        given(canalRepo.findById(10L)).willReturn(Optional.of(canalOnline));
        given(ventaRepo.existsByFechaHoraAndCanal_IdAndReferenciaOrigen(any(), eq(10L), eq("REF-456"))).willReturn(false);
        given(temporadaRepo.findActivaQueContenga(any(LocalDate.class))).willReturn(Optional.empty());
        given(tipoMovRepo.findByCodigo("VENTA")).willReturn(Optional.of(tipoVenta));
        given(usuarioRepo.findById(7L)).willReturn(Optional.of(usuario));
        given(skuRepo.findById(100L)).willReturn(Optional.of(sku1));

        // stock disponible = 5 (insuficiente para 20)
        List<Object[]> stockRows = new ArrayList<>();
        stockRows.add(new Object[]{100L, 5L});
        given(kardexRepo.stockPorSkuHasta(anyCollection(), any(LocalDateTime.class))).willReturn(stockRows);

        // Act + Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> service.registrarVentaManual(dto, 7L));
        assertTrue(ex.getMessage().toLowerCase().contains("stock insuficiente"));

        // No debe persistir cabecera, detalle, ni kardex (precheck antes de guardar)
        then(ventaRepo).should(never()).save(any());
        then(ventaDetRepo).should(never()).save(any());
        then(kardexRepo).should(never()).save(any());
        // Sí debe haberse consultado stock
        then(kardexRepo).should(times(1)).stockPorSkuHasta(anyCollection(), any(LocalDateTime.class));
    }
}
