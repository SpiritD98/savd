package com.colors.savd.repository.projection;

import java.math.BigDecimal;

public interface KpiAggSku {
    Integer getAnio();
    Integer getMes();
    Long getSkuId();
    String getSku();
    String getProducto();
    String getTalla();
    String getColor();
    Long getUnidades();
    BigDecimal getIngresos();
}
