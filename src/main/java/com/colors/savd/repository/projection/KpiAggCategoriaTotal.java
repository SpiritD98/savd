package com.colors.savd.repository.projection;

import java.math.BigDecimal;

public interface KpiAggCategoriaTotal {
    Long getCategoriaId();
    String getCategoria();
    Long getUnidades();
    BigDecimal getIngresos();
}
