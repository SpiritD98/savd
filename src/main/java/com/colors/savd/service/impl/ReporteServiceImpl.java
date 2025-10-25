package com.colors.savd.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colors.savd.dto.AlertaStockDTO;
import com.colors.savd.dto.TopProductoDTO;
import com.colors.savd.model.ParametroReposicion;
import com.colors.savd.repository.KardexRepository;
import com.colors.savd.repository.ParametroReposicionRepository;
import com.colors.savd.repository.VentaRepository;
import com.colors.savd.service.ReporteService;
import com.colors.savd.util.ExcelUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService{

    private final VentaRepository ventaRepo;
    private final KardexRepository kardexRepo;
    private final ParametroReposicionRepository paramRepo;
    private final ExcelUtil excelUtil;

    @Override
    @Transactional(readOnly = true)
    public List<TopProductoDTO> top15(LocalDateTime desde, LocalDateTime hasta, Long canalId) {
        List<Object[]> rows = ventaRepo.top15ByRango(desde, hasta, canalId);
        List<TopProductoDTO> out = new ArrayList<>();
        for(Object[] r : rows){
            Long skuId = (Long) r[0];
            Long unidades = (Long) r[1];
            Double ingresos = (Double) r[2];
            //
            out.add(new TopProductoDTO(skuId, "SKU-"+skuId, "Producto", "Talla", "Color", unidades, BigDecimal.valueOf(ingresos)));
            if (out.size() == 15) break;    
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaStockDTO> alertasStock(LocalDateTime corte) {
        // 1) obtener todos los parámetros de reposición
        List<ParametroReposicion> params = paramRepo.findAll();
        if (params.isEmpty()) return List.of();

        // 2) stock actual por sku hasta 'corte'
        List<Long> skuIds = params.stream().map(p -> p.getSku().getId()).toList();
        var rows = kardexRepo.stockPorSkuHasta(skuIds, corte);

        Map<Long, Long> stockMap = new HashMap<>();
        for (Object[] r : rows) {
            stockMap.put((Long) r[0], (Long) r[1]); // skuId → stock
        }

        // 3) construir alertas (cálculo básico; refinar en siguiente iteración)
        List<AlertaStockDTO> out = new ArrayList<>();
        for (ParametroReposicion p : params) {
            long stock = stockMap.getOrDefault(p.getSku().getId(), 0L);
            int rop = p.getLeadTimeDias() * 1 /*demanda diaria estimada placeholder*/ + p.getStockSeguridad();
            String semaforo = stock <= p.getMinStock() ? "ROJO" : (stock <= rop ? "AMARILLO" : "VERDE");

            out.add(new AlertaStockDTO(
                p.getSku().getId(),
                p.getSku().getSku(),
                (int) stock,
                p.getMinStock(),
                0, // coberturaDias TODO: calcular con ventas promedio
                rop,
                semaforo
            ));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarReporteEjecutivo(LocalDateTime desde, LocalDateTime hasta, Long canalId) {
        var top = top15(desde, hasta, canalId);
        var alertas = alertasStock(null);
        return excelUtil.crearReporteEjecutivo(top, alertas);
    }
}
