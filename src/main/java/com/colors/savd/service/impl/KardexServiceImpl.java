package com.colors.savd.service.impl;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        var ids = skuRepo.findAll().stream().map(s -> s.getId()).toList();
        if (ids.isEmpty()) return Map.of();
        
        var rows = kardexRepo.stockPorSkuHasta(ids, corte);
        Map<Long, Long> stockMap = new HashMap<>();
        for (Object[] r : rows){
            stockMap.put((Long) r[0], (Long) r[1]);
        }
        return stockMap;
    }
}
