package com.colors.savd.repository.projection;

import java.math.BigDecimal;

public interface TopProductoAgg {
    Long getSkuId();
    Long getUnidades();
    BigDecimal getIngresos();
}
