package com.colors.savd.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor
@NoArgsConstructor @Builder
public class LineaVentaRespDTO {
    
    private Long skuId;
    private String sku;
    private String producto;
    private String talla;
    private String color;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal importe;
}
