/*package com.colors.savd.service.impl;

import com.colors.savd.dto.AlertaStockDTO;
import com.colors.savd.dto.TopProductoDTO;
import com.colors.savd.model.ParametroReposicion;
import com.colors.savd.model.VarianteSku;
import com.colors.savd.repository.KardexRepository; // <-- si tu repo se llama KardexMovimientoRepository, cambia import y tipos
import com.colors.savd.repository.ParametroReposicionRepository;
import com.colors.savd.repository.VentaRepository;
import com.colors.savd.util.ExcelUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

  @Mock VentaRepository ventaRepo;

  // Si tu interfaz real es KardexMovimientoRepository, cambia aquí y en el constructor del service.
  @Mock KardexRepository kardexRepo;

  @Mock ParametroReposicionRepository paramRepo;

  // ExcelUtil real para generar XLSX válido
  @Spy ExcelUtil excelUtil = new ExcelUtil();

  @InjectMocks ReporteServiceImpl service;

  @Test
  void top15_mapearYLimitarAQuince() {
    // given: 20 filas simuladas (skuId, unidades, ingresos)
    List<Object[]> rows = new ArrayList<>();
    for (long i = 1; i <= 20; i++) {
      rows.add(new Object[]{ i, 10L + i, 100.0 + i }); // tipos: Long, Long, Double
    }
    when(ventaRepo.top15ByRango(any(), any(), any())).thenReturn(rows);

    // when
    List<TopProductoDTO> out = service.top15(
        LocalDateTime.now().minusDays(7),
        LocalDateTime.now(),
        2L
    );

    // then
    assertNotNull(out);
    assertEquals(15, out.size(), "Debe limitar a 15 elementos");
    // valida mapeo de uno cualquiera
    TopProductoDTO t0 = out.get(0);
    assertEquals(1L, t0.getSkuId());
    assertEquals("SKU-1", t0.getSku());
    assertEquals(new BigDecimal("101.0"), t0.getIngresos());
    verify(ventaRepo, times(1)).top15ByRango(any(), any(), any());
  }

  @Test
  void alertasStock_construyeSemaforoSegunStock() {
    // given
    // parámetros para 2 SKUs
    ParametroReposicion p1 = param(10L, "SKU-10", 5, 3, 2); // min=5, lead=3, ss=2 => rop=5
    ParametroReposicion p2 = param(20L, "SKU-20", 10, 2, 4); // min=10, lead=2, ss=4 => rop=6

    when(paramRepo.findAll()).thenReturn(List.of(p1, p2));

    // stockPorSkuHasta devuelve pares (skuId, stock)
    // p1: stock=4 (<= min 5) => ROJO
    // p2: stock=8 ( > min 10? no, 8 <= 10 => ROJO )  *OJO*: según tu regla, ROJO si stock <= min, AMARILLO si <= rop
    //     Aquí dejamos uno AMARILLO para comprobar los tres colores:
    //     cambiemos: min=10 (p2), ROP=6; si stock=8 -> 8 <= min? sí, sería ROJO.
    //     Para ver AMARILLO, ponemos stock=6 (<= ROP y > min? No, 6 <= 10 sigue ROJO).
    //     Entonces ajustemos p2: min=5, lead=2, ss=4 => ROP=6 y stock=6 => AMARILLO
    ParametroReposicion p2b = param(20L, "SKU-20", 5, 2, 4); // min=5, rop=6
    when(paramRepo.findAll()).thenReturn(List.of(p1, p2b));

    when(kardexRepo.stockPorSkuHasta(eq(List.of(10L, 20L)), any()))
        .thenReturn(List.of(
            new Object[]{10L, 4L}, // ROJO (<= min 5)
            new Object[]{20L, 6L}  // AMARILLO (<= rop=6 y > min=5? aquí es =6 >5 => AMARILLO)
        ));

    // when
    List<AlertaStockDTO> out = service.alertasStock(LocalDateTime.now());

    // then
    assertEquals(2, out.size());

    AlertaStockDTO a1 = out.stream().filter(a -> a.getSkuId() == 10L).findFirst().orElseThrow();
    assertEquals(4, a1.getStockActual());
    assertEquals(5, a1.getMinStock());
    assertEquals(5, a1.getRop()); // 3*1 + 2 = 5 (según tu placeholder)
    assertEquals("ROJO", a1.getSemaforo());

    AlertaStockDTO a2 = out.stream().filter(a -> a.getSkuId() == 20L).findFirst().orElseThrow();
    assertEquals(6, a2.getStockActual());
    assertEquals(5, a2.getMinStock());
    assertEquals(6, a2.getRop()); // 2*1 + 4 = 6
    assertEquals("AMARILLO", a2.getSemaforo());

    verify(paramRepo, times(1)).findAll();
    verify(kardexRepo, times(1)).stockPorSkuHasta(eq(List.of(10L, 20L)), any());
  }

  @Test
  void exportarReporteEjecutivo_retornaXlsxNoVacio() {
    // given
    // Stub de top15
    List<Object[]> rows = new ArrayList<>();
    rows.add(new Object[]{1L, 5L, 150.0});
    when(ventaRepo.top15ByRango(any(), any(), any())).thenReturn(rows);
    // Stub de alertas: param + stock
    ParametroReposicion p1 = param(10L, "SKU-10", 5, 3, 2);
    when(paramRepo.findAll()).thenReturn(List.of(p1));

    List<Object[]> stockRows = new ArrayList<>();
    stockRows.add(new Object[]{10L, 4L});
    when(kardexRepo.stockPorSkuHasta(eq(List.of(10L)), isNull())).thenReturn(stockRows);

    // when
    byte[] bytes = service.exportarReporteEjecutivo(
        LocalDateTime.now().minusDays(30),
        LocalDateTime.now(),
        2L
    );

    // then
    assertNotNull(bytes);
    assertTrue(bytes.length > 100, "El XLSX no debe estar vacío");

    verify(ventaRepo, times(1)).top15ByRango(any(), any(), any());
    verify(paramRepo, times(1)).findAll();
    verify(kardexRepo, times(1)).stockPorSkuHasta(eq(List.of(10L)), any());
  }

  // ===== Helpers =====

  private ParametroReposicion param(Long skuId, String skuCode, int min, int lead, int ss) {
    VarianteSku v = new VarianteSku();
    v.setId(skuId);
    v.setSku(skuCode);
    v.setPrecioLista(new BigDecimal("0"));

    ParametroReposicion p = new ParametroReposicion();
    p.setId(skuId);
    p.setSku(v);
    p.setMinStock(min);
    p.setLeadTimeDias(lead);
    p.setStockSeguridad(ss);
    return p;
  }
}*/