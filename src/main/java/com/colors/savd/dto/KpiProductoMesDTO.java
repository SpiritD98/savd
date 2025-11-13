package com.colors.savd.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class KpiProductoMesDTO {
    private int anio;
    private int mes;

    private Long productoId;
    private String producto;

    private Long unidades;
    private BigDecimal ingresos; 

    private BigDecimal aporteMes; // proporción 0..1

    private BigDecimal varMesAnteriorUnidades;
    private BigDecimal varMesAnteriorIngresos;
    
    private BigDecimal varYoYUnidades;
    private BigDecimal varYoYIngresos;
}
