package com.colors.savd.repository.projection;

import java.math.BigDecimal;

public interface KpiAggProductoTotal {
    Long getProductoId();
    String getProducto();
    Long getUnidades();
    BigDecimal getIngresos();
}
