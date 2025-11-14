package com.colors.savd.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class KpiProductoDTO {
    private Long productoId;
    private String producto;
    private Long unidades;
    private BigDecimal ingresos;
    private BigDecimal aportePct;      // ingresos/total *100
    private BigDecimal variacionMesPct; // vs periodo anterior igual duración
    private BigDecimal variacionYoYPct; // vs mismo periodo año anterior
}
