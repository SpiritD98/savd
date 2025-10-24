package com.colors.savd.service;

import java.time.LocalDateTime;
import java.util.Map;

public interface KardexService {
    /**
     * Calcula stock actual por SKU hasta una fecha de corte. (si corte == null, usar now)
     * @param corte fecha de corte
     * @return mapa skuId -> stock
     */
    Map<Long, Long> stockPorSkuHasta(LocalDateTime corte);
}
