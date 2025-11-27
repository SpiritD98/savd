package com.colors.savd.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.colors.savd.config.CacheConfig; // tu clase @Configuration @EnableCaching
import com.colors.savd.dto.KpiProductoMesDTO;
import com.colors.savd.repository.KpiRepository;
import com.colors.savd.repository.projection.KpiAggProducto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(CacheConfig.class) // asegura el CacheManager de Caffeine en el test
class ReporteServiceCacheTest {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private KpiRepository kpiRepo;

    // Proyección dummy para kpiProductoMensual
    interface KP extends KpiAggProducto {}

    private KpiAggProducto mockRow(int anio, int mes, long prodId, String prod, long unidades, BigDecimal ingresos) {
        KP row = Mockito.mock(KP.class);
        given(row.getAnio()).willReturn(anio);
        given(row.getMes()).willReturn(mes);
        given(row.getProductoId()).willReturn(prodId);
        given(row.getProducto()).willReturn(prod);
        given(row.getUnidades()).willReturn(unidades);
        given(row.getIngresos()).willReturn(ingresos);
        return row;
    }

    @Test
    @DisplayName("Cache hit: misma llamada dos veces → repo se invoca 1 sola vez")
    @Transactional(readOnly = true)
    void cacheHit_mismaLlave_repoSoloUnaVez() {
        // Arrange
        var desde = LocalDateTime.of(2025, 1, 1, 10, 15, 12);
        var hasta = LocalDateTime.of(2025, 1, 31, 23, 59, 59);
        var row = mockRow(2025, 1, 101L, "Polo Básico", 120L, new BigDecimal("1500.00"));

        given(kpiRepo.kpiProductoMensual(any(), any(), any(), any(), any(), any(), any()))
            .willReturn(List.of(row));

        // Act 1: primer llamado (cache miss)
        List<KpiProductoMesDTO> r1 = reporteService.kpiProductoMensual(
            desde, hasta, null, null, null, null, null);

        // Act 2: segundo llamado, mismos params (cache hit)
        List<KpiProductoMesDTO> r2 = reporteService.kpiProductoMensual(
            desde, hasta, null, null, null, null, null);

        // Assert
        then(kpiRepo).should(times(1))
            .kpiProductoMensual(any(), any(), any(), any(), any(), any(), any());
        // mismas referencias (o al menos tamaño igual): confirma mismo resultado cacheado
        org.assertj.core.api.Assertions.assertThat(r1).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(r2).hasSize(1);
    }

    @Test
    @DisplayName("Normalización de fechas: segundos distintos, misma llave de caché")
    @Transactional(readOnly = true)
    void normalizaFechas_mismoDia_mismaLlave() {
        // Arrange: dos rangos con segundos distintos pero mismo día
        var desdeA = LocalDateTime.of(2025, 2, 1, 0, 0, 5);
        var hastaA = LocalDateTime.of(2025, 2, 28, 23, 59, 58);

        var desdeB = LocalDateTime.of(2025, 2, 1, 0, 0, 59);
        var hastaB = LocalDateTime.of(2025, 2, 28, 23, 59, 10);

        var row = mockRow(2025, 2, 201L, "Camisa Slim", 80L, new BigDecimal("2100.00"));
        given(kpiRepo.kpiProductoMensual(any(), any(), any(), any(), any(), any(), any()))
            .willReturn(List.of(row));

        // Act: primera llamada (miss)
        reporteService.kpiProductoMensual(desdeA, hastaA, null, null, null, null, null);
        // Act: segunda llamada con segundos diferentes (hit si tus @Cacheable normalizan)
        reporteService.kpiProductoMensual(desdeB, hastaB, null, null, null, null, null);

        // Assert: repo invocado una sola vez si la clave se normaliza (dayStart/dayEnd o trunc)
        then(kpiRepo).should(times(1))
            .kpiProductoMensual(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Evicción manual: limpiar caché fuerza nueva consulta al repo")
    @Transactional(readOnly = true)
    void eviccionManual_clearVuelveAInvocarRepo() {
        var desde = LocalDateTime.of(2025, 3, 1, 0, 0, 0);
        var hasta = LocalDateTime.of(2025, 3, 31, 23, 59, 59);

        var row = mockRow(2025, 3, 301L, "Jeans Regular", 60L, new BigDecimal("1800.00"));
        given(kpiRepo.kpiProductoMensual(any(), any(), any(), any(), any(), any(), any()))
            .willReturn(List.of(row));

        // Prime cache
        reporteService.kpiProductoMensual(desde, hasta, 1L, null, null, null, null);
        then(kpiRepo).should(times(1))
            .kpiProductoMensual(any(), any(), any(), any(), any(), any(), any());

        // Evict cache programáticamente (simula importación persistente que limpia)
        var cache = cacheManager.getCache("kpiProductoMensual");
        if (cache != null) cache.clear();

        // Llamada otra vez → debe invocar repo de nuevo
        reporteService.kpiProductoMensual(desde, hasta, 1L, null, null, null, null);
        then(kpiRepo).should(times(2))
            .kpiProductoMensual(any(), any(), any(), any(), any(), any(), any());
    }
}
