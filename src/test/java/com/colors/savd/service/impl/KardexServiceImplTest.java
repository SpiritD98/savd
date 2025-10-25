package com.colors.savd.service.impl;

import com.colors.savd.model.VarianteSku;
import com.colors.savd.repository.KardexRepository; // si usas KardexMovimientoRepository, cambia import y tipo
import com.colors.savd.repository.VarianteSkuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KardexServiceImplTest {

  @Mock KardexRepository kardexRepo;                // <— cambia si tu interfaz es KardexMovimientoRepository
  @Mock VarianteSkuRepository skuRepo;

  @InjectMocks KardexServiceImpl service;

  @Test
  void stockPorSkuHasta_sinSkus_retornaMapaVacio_yNoConsultaKardex() {
    // given
    when(skuRepo.findAll()).thenReturn(Collections.emptyList());

    // when
    Map<Long, Long> out = service.stockPorSkuHasta(LocalDateTime.now());

    // then
    assertNotNull(out);
    assertTrue(out.isEmpty(), "Debe retornar mapa vacío si no hay SKUs");
    verify(kardexRepo, never()).stockPorSkuHasta(anyList(), any());
  }

  @Test
  void stockPorSkuHasta_conSkus_mapeaFilasCorrectamente() {
    // given
    VarianteSku v1 = new VarianteSku(); v1.setId(10L);
    VarianteSku v2 = new VarianteSku(); v2.setId(20L);
    when(skuRepo.findAll()).thenReturn(List.of(v1, v2));

    LocalDateTime corte = LocalDateTime.now();

    // el repo devuelve pares (skuId, stock)
    List<Object[]> rows = new ArrayList<>();
    rows.add(new Object[]{10L, 7L});
    rows.add(new Object[]{20L, 0L});

    when(kardexRepo.stockPorSkuHasta(eq(List.of(10L, 20L)), eq(corte))).thenReturn(rows);

    // when
    Map<Long, Long> out = service.stockPorSkuHasta(corte);

    // then
    assertNotNull(out);
    assertEquals(2, out.size());
    assertEquals(7L, out.get(10L));
    assertEquals(0L, out.get(20L));

    verify(skuRepo, times(1)).findAll();
    verify(kardexRepo, times(1)).stockPorSkuHasta(eq(List.of(10L, 20L)), eq(corte));
  }
}
