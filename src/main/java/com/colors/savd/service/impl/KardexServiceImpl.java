package com.colors.savd.service.impl;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colors.savd.model.VarianteSku;
import com.colors.savd.repository.KardexRepository;
import com.colors.savd.repository.VarianteSkuRepository;
import com.colors.savd.service.KardexService;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KardexServiceImpl implements KardexService{

    private final KardexRepository kardexRepo;
    private final VarianteSkuRepository skuRepo;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> stockPorSkuHasta(LocalDateTime corte) {
        // 1) Fecha de corte efectiva (si null → ahora), normalizada a segundos
        LocalDateTime corteEf = (corte != null ? corte : LocalDateTime.now()).withNano(0);

        // 2) Obtener IDs de SKUs activos (puedes cambiar a findAll() si quieres todos)
        var ids = skuRepo.findByActivoTrue().stream().map(VarianteSku::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        
        // 3) Consultar stock agregado por SKU hasta la fecha de corte
        var rows = kardexRepo.stockPorSkuHasta(ids, corteEf);
        Map<Long, Long> stockMap = new HashMap<>();
        for (Object[] r : rows){
            // r[0] = skuId, r[1] = stock → usarlos como Number para evitar ClassCastException
            Number skuN = (Number) r[0];
            Number stockN = (Number) r[1];

            if (skuN == null) {
                continue; // fila rara, la ignoramos
            }

            long skuId = skuN.longValue();
            long stock = (stockN != null ? stockN.longValue() : 0L);
            stockMap.put(skuId, stock);
        }
        
        return stockMap;
    }
}
