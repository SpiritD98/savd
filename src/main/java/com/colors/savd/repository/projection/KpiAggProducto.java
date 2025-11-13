package com.colors.savd.repository.projection;

import java.math.BigDecimal;

public interface KpiAggProducto {
    Integer getAnio();
    Integer getMes();
    Long getProductoId();
    String getProducto();
    Long getUnidades();
    BigDecimal getIngresos();
}
