package com.colors.savd.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class KpiCategoriaMesDTO {

    private int anio;
    private int mes; // 1..12

    private Long categoriaId;
    private String categoria;

    private Long unidades;
    private BigDecimal ingresos;

    // Aporte dentro del mes (0..1)
    private BigDecimal aporteMes;

    // Variaciones (porcentaje -1..+inf), null si no aplica
    private BigDecimal varMesAnteriorUnidades;
    private BigDecimal varMesAnteriorIngresos;

    private BigDecimal varYoYUnidades;  // vs mismo mes año previo
    private BigDecimal varYoYIngresos;

}
