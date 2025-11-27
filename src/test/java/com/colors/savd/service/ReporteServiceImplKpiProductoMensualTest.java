package com.colors.savd.service;

import com.colors.savd.dto.KpiProductoMesDTO;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.repository.*;
import com.colors.savd.repository.projection.KpiAggProducto;
import com.colors.savd.service.impl.ReporteServiceImpl;
import com.colors.savd.util.ExcelUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReporteServiceImplKpiProductoMensualTest {

    // Solo se usa kpiRepo en kpiProductoMensual; el resto se mockea para el constructor
    @Mock private VentaRepository ventaRepo;
    @Mock private KardexRepository kardexRepo;
    @Mock private ParametroReposicionRepository paramRepo;
    @Mock private VarianteSkuRepository varianteSkuRepo;
    @Mock private ExcelUtil excelUtil;
    @Mock private KpiRepository kpiRepo;

    @InjectMocks
    private ReporteServiceImpl service;

    // ----- helper para crear filas de proyección -----
    private static KpiAggProducto row(Integer anio, Integer mes, Long productoId, String producto, Long unidades, BigDecimal ingresos) {
        return new KpiAggProducto() {
            @Override public Integer getAnio() { return anio; }
            @Override public Integer getMes() { return mes; }
            @Override public Long getProductoId() { return productoId; }
            @Override public String getProducto() { return producto; }
            @Override public Long getUnidades() { return unidades; }
            @Override public BigDecimal getIngresos() { return ingresos; }
        };
    }

    @Test
    @DisplayName("kpiProductoMensual: calcula aporteMes, variación mes-1 y YoY correctamente")
    void kpiProductoMensual_ok() {
        // Dataset: incluimos ene-2024 (para YoY ene-2025), ene-2025 y feb-2025
        // Ene-2024 (YoY base)
        var pA_2024_01 = row(2024, 1, 10L, "Prod A",  8L,  new BigDecimal("80"));
        var pB_2024_01 = row(2024, 1, 20L, "Prod B", 20L,  new BigDecimal("200"));

        // Ene-2025: total ingresos = 100 + 300 = 400  => aportes: A=0.25, B=0.75; YoY vs ene-2024
        var pA_2025_01 = row(2025, 1, 10L, "Prod A", 10L,  new BigDecimal("100"));
        var pB_2025_01 = row(2025, 1, 20L, "Prod B", 30L,  new BigDecimal("300"));

        // Feb-2025: total = 150 + 150 = 300  => aportes: A=0.5, B=0.5; variación vs ene-2025
        var pA_2025_02 = row(2025, 2, 10L, "Prod A", 15L,  new BigDecimal("150"));
        var pB_2025_02 = row(2025, 2, 20L, "Prod B", 15L,  new BigDecimal("150"));

        given(kpiRepo.kpiProductoMensual(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(
                        pA_2024_01, pB_2024_01,
                        pA_2025_01, pB_2025_01,
                        pA_2025_02, pB_2025_02
                ));

        var desde = LocalDateTime.of(2024, 1, 1, 0, 0);
        var hasta = LocalDateTime.of(2025, 2, 28, 23, 59);

        List<KpiProductoMesDTO> out = service.kpiProductoMensual(desde, hasta, null, null, null, null, null);

        // Verificamos que existan las 6 filas
        assertThat(out).hasSize(6);

        // Buscamos DTOs clave
        var ene25_A = out.stream().filter(d -> d.getAnio()==2025 && d.getMes()==1 && d.getProductoId()==10L).findFirst().orElseThrow();
        var ene25_B = out.stream().filter(d -> d.getAnio()==2025 && d.getMes()==1 && d.getProductoId()==20L).findFirst().orElseThrow();
        var feb25_A = out.stream().filter(d -> d.getAnio()==2025 && d.getMes()==2 && d.getProductoId()==10L).findFirst().orElseThrow();
        var feb25_B = out.stream().filter(d -> d.getAnio()==2025 && d.getMes()==2 && d.getProductoId()==20L).findFirst().orElseThrow();

        // --- Aportes Ene-2025 ---
        // A: 100 / 400 = 0.25
        assertThat(ene25_A.getAporteMes()).isNotNull();
        assertThat(ene25_A.getAporteMes().compareTo(new BigDecimal("0.25"))).isZero();
        // B: 300 / 400 = 0.75
        assertThat(ene25_B.getAporteMes()).isNotNull();
        assertThat(ene25_B.getAporteMes().compareTo(new BigDecimal("0.75"))).isZero();

        // --- YoY Ene-2025 vs Ene-2024 ---
        // A: (100 - 80) / 80 = 0.25
        assertThat(ene25_A.getVarYoYIngresos()).isNotNull();
        assertThat(ene25_A.getVarYoYIngresos().compareTo(new BigDecimal("0.25"))).isZero();
        // B: (300 - 200) / 200 = 0.50
        assertThat(ene25_B.getVarYoYIngresos()).isNotNull();
        assertThat(ene25_B.getVarYoYIngresos().compareTo(new BigDecimal("0.50"))).isZero();

        // --- Feb-2025: aportes 0.5 / 0.5 ---
        assertThat(feb25_A.getAporteMes().compareTo(new BigDecimal("0.5"))).isZero();
        assertThat(feb25_B.getAporteMes().compareTo(new BigDecimal("0.5"))).isZero();

        // --- Variación vs mes anterior (Feb-2025 vs Ene-2025) ---
        // A: (150 - 100) / 100 = 0.50
        assertThat(feb25_A.getVarMesAnteriorIngresos()).isNotNull();
        assertThat(feb25_A.getVarMesAnteriorIngresos().compareTo(new BigDecimal("0.50"))).isZero();
        // B: (150 - 300) / 300 = -0.50
        assertThat(feb25_B.getVarMesAnteriorIngresos()).isNotNull();
        assertThat(feb25_B.getVarMesAnteriorIngresos().compareTo(new BigDecimal("-0.50"))).isZero();

        // Feb-2025 YoY: no hay feb-2024 -> null
        assertThat(feb25_A.getVarYoYIngresos()).isNull();
        assertThat(feb25_B.getVarYoYIngresos()).isNull();
    }

    @Test
    @DisplayName("kpiProductoMensual: valida rango y lanza BusinessException si 'desde' > 'hasta'")
    void kpiProductoMensual_rangoInvalido() {
        var desde = LocalDateTime.of(2025, 2, 1, 0, 0);
        var hasta = LocalDateTime.of(2025, 1, 31, 23, 59);

        assertThrows(BusinessException.class, () ->
                service.kpiProductoMensual(desde, hasta, null, null, null, null, null));
    }
}
