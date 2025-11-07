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
import com.colors.savd.exception.BusinessException;
import com.colors.savd.model.ParametroReposicion;
import com.colors.savd.model.VarianteSku;
import com.colors.savd.repository.KardexRepository;
import com.colors.savd.repository.ParametroReposicionRepository;
import com.colors.savd.repository.VarianteSkuRepository;
import com.colors.savd.repository.VentaRepository;
import com.colors.savd.repository.projection.TopProductoAgg;
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
        // validar y normalizar fechas
        validarRangoFechas(desde, hasta);
        LocalDateTime desdeEf = desde.withNano(0);
        LocalDateTime hastaEf = hasta.withNano(0);

        // 1) Traemos el TOP con proyeccoon y limite 15
        var rows = ventaRepo.top15ByRango(desdeEf, hastaEf, canalId, PageRequest.of(0, 15));
        if (rows == null || rows.isEmpty())  return List.of();          

        // 2) Obtenemos info de variantes en bloque (para nombres reales)
        List<Long> skuIds = rows.stream().map(TopProductoAgg::getSkuId).distinct().toList();

        // Usamos el método específico del repo (ya definido)
        Map<Long, VarianteSku> skuMap = varianteSkuRepo.findByIdIn(skuIds).stream().collect(Collectors.toMap(VarianteSku::getId, v -> v));

        // 3) Construimos DTOs reales
        List<TopProductoDTO> out = new ArrayList<>(rows.size());
        for(TopProductoAgg r : rows){
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
        LocalDateTime corteEf = (corte != null ? corte : LocalDateTime.now()).withNano(0);

        // 1) obtener todos los parámetros de reposición
        List<ParametroReposicion> params = paramRepo.findAll();
        if (params.isEmpty()) return List.of();

        // 2) stock actual por sku hasta 'corte'
        List<Long> skuIds = params.stream().map(p -> p.getSku().getId()).toList();
        var rowsStock = kardexRepo.stockPorSkuHasta(skuIds, corteEf);

        Map<Long, Long> stockMap = new HashMap<>();
        for (Object[] r : rowsStock) {
            Number skuN = (Number) r[0];
            Number stN = (Number) r[1];
            long skuId = skuN == null ? 0L : skuN.longValue();
            long stock = stN == null ? 0L : stN.longValue();
            stockMap.put(skuId, stock);
        }

        // 3) estimar demanda diaria por SKU en una ventana reciente (ej. 28 días)
        int diasVentana = 28;
        LocalDateTime desdeVentana = corteEf.minusDays(diasVentana);
        
        var rowsVentas = kardexRepo.ventasPorSkuEnRango(skuIds, desdeVentana, corteEf);
        Map<Long, Long> ventasMap = new HashMap<>();
        for (Object[] r : rowsVentas) {
            Number skuN = (Number) r[0];
            Number vendN = (Number) r[1];
            long skuId = skuN == null ? 0L : skuN.longValue();
            long vendidas = vendN == null ? 0L : vendN.longValue();
            ventasMap.put(skuId, vendidas);
        }

        // 4) construir alertas usando demanda estimada        
        List<AlertaStockDTO> out = new ArrayList<>();
        for (ParametroReposicion p : params) {
            long skuId = p.getSku().getId();
            long stock = stockMap.getOrDefault(skuId, 0L);
            long vendidasVentana = ventasMap.getOrDefault(skuId, 0L);

            // demanda diaria minima = 1 si hubo algo de venta; si cero, puedes dejar 1
            int demandaDiaria;
            if (vendidasVentana <= 0) {
                demandaDiaria = 1; // no hay ventas recientes → demanda baja, pero evitamos división por cero
            }else{
                demandaDiaria = (int) Math.max(1, Math.ceil(vendidasVentana / (double) diasVentana));
            }

            // cobertura en días: stock actual / demanda diaria
            int coberturaDias = (demandaDiaria > 0) ? (int) Math.floor(stock / (double) demandaDiaria) : 0;
            int rop = demandaDiaria * p.getLeadTimeDias() + p.getStockSeguridad();

            String semaforo = (stock <= p.getMinStock()) ? "ROJO" : (stock <= rop ? "AMARILLO" : "VERDE");

            out.add(AlertaStockDTO.builder()
            .skuId(skuId).sku(nvl(p.getSku().getSku()))
            .stockActual((int) stock)
            .minStock(p.getMinStock())
            .coberturaDias(coberturaDias)
            .rop(rop)
            .semaforo(semaforo)
            .build());
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarReporteEjecutivo(LocalDateTime desde, LocalDateTime hasta, Long canalId) {
        // validamos también aquí por si llaman directo
        validarRangoFechas(desde, hasta);
        LocalDateTime desdeEf = desde.withNano(0);
        LocalDateTime hastaEf = hasta.withNano(0);

        List<TopProductoDTO> top = top15(desdeEf, hastaEf, canalId);
        //usamos el mismo corte de "hasta" para stock actual del reporte
        List<AlertaStockDTO> alertas = alertasStock(hastaEf);
        return excelUtil.crearReporteEjecutivo(top, alertas);
    }

    // ================= Helpers privados =================
    private static String nvl (String s) { return s == null ? "" : s; }

    private void validarRangoFechas(LocalDateTime desde, LocalDateTime hasta){
        if (desde == null || hasta == null) {
            throw new BusinessException("Debe especificar las fechas 'desde' y 'hasta'.");
        }
        if (desde.isAfter(hasta)) {
            throw new BusinessException("'desde' no puede ser mayor que 'hasta'.");
        }
    }
}
