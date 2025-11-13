package com.colors.savd.repository.projection;

import java.math.BigDecimal;

public interface KpiAggCategoria {
    Integer getAnio();
    Integer getMes();

    Long getCategoriaId();
    String getCategoria();

    Long getUnidades();
    BigDecimal getIngresos();
}
