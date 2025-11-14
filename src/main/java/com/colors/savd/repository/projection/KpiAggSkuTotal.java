package com.colors.savd.repository.projection;

import java.math.BigDecimal;

public interface KpiAggSkuTotal {
    Long getSkuId();
    String getSku();
    String getProducto();
    String getTalla();
    String getColor();
    Long getUnidades();
    BigDecimal getIngresos();
}
