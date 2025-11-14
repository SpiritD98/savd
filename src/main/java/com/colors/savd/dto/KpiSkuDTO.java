package com.colors.savd.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class KpiSkuDTO {
    private Long skuId;
    private String sku;
    private String producto;
    private String talla;
    private String color;
    private Long unidades;
    private BigDecimal ingresos;
    private BigDecimal aportePct;
    private BigDecimal variacionMesPct;
    private BigDecimal variacionYoYPct;
}
