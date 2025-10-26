package com.colors.savd.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colors.savd.dto.AlertaStockDTO;
import com.colors.savd.dto.TopProductoDTO;
import com.colors.savd.model.ParametroReposicion;
import com.colors.savd.model.VarianteSku;
import com.colors.savd.repository.KardexRepository;
import com.colors.savd.repository.ParametroReposicionRepository;
import com.colors.savd.repository.VarianteSkuRepository;
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
    private final VarianteSkuRepository varianteSkuRepo;
    private final ExcelUtil excelUtil;

    @Override
    @Transactional(readOnly = true)
    public List<TopProductoDTO> top15(LocalDateTime desde, LocalDateTime hasta, Long canalId) {
        // 1) Traemos el TOP con proyeccoon y limite 15
        var rows = ventaRepo.top15ByRango(desde, hasta, canalId, PageRequest.of(0, 15));
        if (rows == null || rows.isEmpty())  return List.of();          

        // 2) Obtenemos info de variantes en bloque (para nombres reales)
        List<Long> skuIds = rows.stream().map(r -> r.getSkuId()).distinct().toList();
        Map<Long, VarianteSku> skuMap = varianteSkuRepo.findAllById(skuIds).stream().collect(Collectors.toMap(VarianteSku::getId, v -> v));

        // 3) Construimos DTOs reales
        List<TopProductoDTO> out = new ArrayList<>(rows.size());
        for(var r : rows){
            Long skuId = r.getSkuId();
            Long unidades = r.getUnidades();
            BigDecimal ingresos = r.getIngresos();

            VarianteSku v = skuMap.get(skuId);
            if (v == null) {
                // Si el SKU ya no existe (borrado lógico), lo omitimos o lo marcamos
                out.add(new TopProductoDTO(skuId, "SKU-"+ skuId, "(sin producto)", "", "", 
                unidades != null ? unidades : 0L, ingresos != null ? ingresos : BigDecimal.ZERO));
                continue;
            }
            
            out.add(new TopProductoDTO(skuId, nvl(v.getSku()),
                v.getProducto() != null ? nvl(v.getProducto().getNombre()) : "",
                v.getTalla() != null ? nvl (v.getTalla().getCodigo()) : "",
                v.getColor() != null ? nvl (v.getColor().getNombre()) : "",
                unidades != null ? unidades : 0L,
                ingresos != null ? ingresos : BigDecimal.ZERO
            ));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaStockDTO> alertasStock(LocalDateTime corte) {
        
        // si no mandan corte, usamos "ahora"
        LocalDateTime corteEf = (corte != null) ? corte : LocalDateTime.now();

        // 1) obtener todos los parámetros de reposición
        List<ParametroReposicion> params = paramRepo.findAll();
        if (params.isEmpty()) return List.of();

        // 2) stock actual por sku hasta 'corte'
        List<Long> skuIds = params.stream().map(p -> p.getSku().getId()).toList();
        var rows = kardexRepo.stockPorSkuHasta(skuIds, corteEf);

        Map<Long, Long> stockMap = new HashMap<>();
        for (Object[] r : rows) {
            Number skuN = (Number) r[0];
            Number stN = (Number) r[1];
            long skuId = skuN == null ? 0L : skuN.longValue();
            long stock = stN == null ? 0L : stN.longValue();
            stockMap.put(skuId, stock);
        }

        // 3) construir alertas (demanda diaria = TODO: refinar con promedio)
        List<AlertaStockDTO> out = new ArrayList<>();
        for (ParametroReposicion p : params) {
            long skuId = p.getSku().getId();
            long stock = stockMap.getOrDefault(skuId, 0L);

            int demandaDiaria = 1; //TODO: estimar por ventas ultimas x semanas
            int rop = demandaDiaria * p.getLeadTimeDias() + p.getStockSeguridad();

            String semaforo = (stock <= p.getMinStock()) ? "ROJO" : (stock <= rop ? "AMARILLO" : "VERDE");

            out.add(new AlertaStockDTO(
                skuId,
                nvl(p.getSku().getSku()),
                (int) stock,
                p.getMinStock(),
                0, //coberturaDias (TODO: con demanda estimada)
                rop,
                semaforo
            ));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarReporteEjecutivo(LocalDateTime desde, LocalDateTime hasta, Long canalId) {
        List<TopProductoDTO> top = top15(desde, hasta, canalId);
        //usamos el mismo corte de "hasta" para stock actual del reporte
        List<AlertaStockDTO> alertas = alertasStock(hasta);
        return excelUtil.crearReporteEjecutivo(top, alertas);
    }

    private static String nvl (String s) { return s == null ? "" : s; }
}
