package com.colors.savd.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class KpiCategoriaDTO {
    private Long categoriaId;
    private String categoria;
    private Long unidades;            // periodo actual
    private BigDecimal ingresos;      // periodo actual
    private BigDecimal aportePct;     // (ingresos / total periodo) * 100, null si total=0
    private BigDecimal variacionMesPct;  // vs periodo anterior (misma duración), null si base=0
    private BigDecimal variacionYoYPct;  // vs mismo periodo año anterior, null si base=0
}
