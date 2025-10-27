package com.colors.savd.repository.projection;

import java.math.BigDecimal;

public interface ResumenVentaAgg {
    Long getVentaId();
    Long getUnidades();
    BigDecimal getImporte();
}
