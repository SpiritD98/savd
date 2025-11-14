package com.colors.savd.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colors.savd.dto.AlertaStockDTO;
import com.colors.savd.dto.KpiCategoriaDTO;
import com.colors.savd.dto.KpiCategoriaMesDTO;
import com.colors.savd.dto.KpiProductoDTO;
import com.colors.savd.dto.KpiProductoMesDTO;
import com.colors.savd.dto.KpiSkuDTO;
import com.colors.savd.dto.KpiSkuMesDTO;
import com.colors.savd.dto.TopProductoDTO;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.model.ParametroReposicion;
import com.colors.savd.model.VarianteSku;
import com.colors.savd.repository.KardexRepository;
import com.colors.savd.repository.KpiRepository;
import com.colors.savd.repository.ParametroReposicionRepository;
import com.colors.savd.repository.VarianteSkuRepository;
import com.colors.savd.repository.VentaRepository;
import com.colors.savd.repository.projection.KpiAggCategoriaTotal;
import com.colors.savd.repository.projection.KpiAggProductoTotal;
import com.colors.savd.repository.projection.KpiAggSkuTotal;
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
    private final KpiRepository kpiRepo;

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

    @Override
    @Transactional(readOnly = true)
    public List<KpiCategoriaMesDTO> kpiCategoriaMensual(LocalDateTime desde, LocalDateTime hasta, Long canalId,
            Long temporadaId, Long categoriaId, Long tallaId, Long colorId) {

        // 1) Traer agregación cruda        
        var rows = kpiRepo.kpiCategoriaMensual(desde,hasta,canalId,temporadaId,categoriaId,tallaId,colorId);
        if (rows == null || rows.isEmpty()) return List.of();

        // 2) Totales por (año, mes) para calcular aporte
        record Ym(int y, int m) {}
        Map<Ym, BigDecimal> totalIngresosMes = new HashMap<>();
        Map<Ym, Long> totalUnidadesMes = new HashMap<>();

        for (var r : rows) {
            Ym ym = new Ym(nz(r.getAnio()), nz(r.getMes()));
            totalIngresosMes.merge(ym, nzBD(r.getIngresos()), BigDecimal::add);
            totalUnidadesMes.merge(ym, nzL(r.getUnidades()), Long::sum);
        }

        // 3) Indexar por (categoriaId, anio, mes) para variaciones
        record Key(Long cat, int y, int m) {}
        Map<Key, KpiCategoriaMesDTO> byKey = new HashMap<>();
        List<KpiCategoriaMesDTO> out = new ArrayList<>(rows.size());

        for (var r : rows) {
            int anio = nz(r.getAnio());
            int mes  = nz(r.getMes());
            Ym ym = new Ym(anio, mes);

            Long unidades  = nzL(r.getUnidades());
            BigDecimal ingresos = nzBD(r.getIngresos());
            BigDecimal totalMes = totalIngresosMes.getOrDefault(ym, BigDecimal.ZERO);

            BigDecimal aporte = totalMes.signum() == 0 
                    ? BigDecimal.ZERO 
                    : ingresos.divide(totalMes, 6, RoundingMode.HALF_UP); // proporción 0..1

            var dto = KpiCategoriaMesDTO.builder()
                    .anio(anio)
                    .mes(mes)
                    .categoriaId(r.getCategoriaId())
                    .categoria(r.getCategoria())
                    .unidades(unidades)
                    .ingresos(ingresos)
                    .aporteMes(aporte)
                    .build();

            out.add(dto);
            byKey.put(new Key(r.getCategoriaId(), anio, mes), dto);
        }

        // 4) Variaciones vs mes anterior y YoY
        for (var dto : out) {
            int anio = dto.getAnio();
            int mes  = dto.getMes();
            Long cat = dto.getCategoriaId();

            // Mes anterior
            int prevY = anio, prevM = mes - 1;
            if (prevM == 0) { prevM = 12; prevY = anio - 1; }
            var prev = byKey.get(new Key(cat, prevY, prevM));
            if (prev != null) {
                dto.setVarMesAnteriorUnidades( varPct(dto.getUnidades(), prev.getUnidades()) );
                dto.setVarMesAnteriorIngresos( varPct(dto.getIngresos(), prev.getIngresos()) );
            }

            // YoY (mismo mes, año - 1)
            var yoy = byKey.get(new Key(cat, anio - 1, mes));
            if (yoy != null) {
                dto.setVarYoYUnidades( varPct(dto.getUnidades(), yoy.getUnidades()) );
                dto.setVarYoYIngresos( varPct(dto.getIngresos(), yoy.getIngresos()) );
            }
        }

        // Orden opcional final: por año/mes y mayor aporte
        out.sort(Comparator
            .comparing(KpiCategoriaMesDTO::getAnio)
            .thenComparing(KpiCategoriaMesDTO::getMes)
            .thenComparing(KpiCategoriaMesDTO::getAporteMes, Comparator.nullsLast(Comparator.reverseOrder()))
        );

        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KpiProductoMesDTO> kpiProductoMensual(LocalDateTime desde, LocalDateTime hasta, Long canalId,
            Long temporadaId, Long categoriaId, Long tallaId, Long colorId) {
        var rows = kpiRepo.kpiProductoMensual(desde, hasta, canalId, temporadaId, categoriaId, tallaId, colorId);
        if (rows == null || rows.isEmpty()) return List.of();

        record Ym(int y, int m) {}
        record Key(Long id, int y, int m) {}

        // 1) Totales por mes (ingresos) para aporte
        Map<Ym, BigDecimal> totalIngresosMes = new HashMap<>();
        for (var r : rows) {
            Ym ym = new Ym(nz(r.getAnio()), nz(r.getMes()));
            totalIngresosMes.merge(ym, nzBD(r.getIngresos()), BigDecimal::add);
        }

        // 2) Construcción de DTO + index para variaciones
        Map<Key, KpiProductoMesDTO> index = new HashMap<>();
        List<KpiProductoMesDTO> out = new ArrayList<>(rows.size());

        for (var r : rows) {
            int anio = nz(r.getAnio());
            int mes  = nz(r.getMes());
            Ym ym = new Ym(anio, mes);

            BigDecimal ingresos = nzBD(r.getIngresos());
            BigDecimal aporte = totalIngresosMes.getOrDefault(ym, BigDecimal.ZERO).signum() == 0
                    ? BigDecimal.ZERO
                    : ingresos.divide(totalIngresosMes.get(ym), 6, RoundingMode.HALF_UP);

            var dto = KpiProductoMesDTO.builder()
                    .anio(anio)
                    .mes(mes)
                    .productoId(r.getProductoId())
                    .producto(r.getProducto())
                    .unidades(nzL(r.getUnidades()))
                    .ingresos(ingresos)
                    .aporteMes(aporte)
                    .build();

            out.add(dto);
            index.put(new Key(r.getProductoId(), anio, mes), dto);
        }

        // 3) Variaciones Mes-1 y YoY
        for (var dto : out) {
            int y = dto.getAnio(), m = dto.getMes();
            Long id = dto.getProductoId();

            int prevY = y, prevM = m - 1;
            if (prevM == 0) { prevM = 12; prevY = y - 1; }

            var prev = index.get(new Key(id, prevY, prevM));
            if (prev != null) {
                dto.setVarMesAnteriorUnidades( varPct(dto.getUnidades(), prev.getUnidades()) );
                dto.setVarMesAnteriorIngresos( varPct(dto.getIngresos(), prev.getIngresos()) );
            }

            var yoy = index.get(new Key(id, y - 1, m));
            if (yoy != null) {
                dto.setVarYoYUnidades( varPct(dto.getUnidades(), yoy.getUnidades()) );
                dto.setVarYoYIngresos( varPct(dto.getIngresos(), yoy.getIngresos()) );
            }
        }

        ordenarKpi(out, KpiProductoMesDTO::getAporteMes);
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KpiSkuMesDTO> kpiSkuMensual(LocalDateTime desde, LocalDateTime hasta, Long canalId, Long temporadaId,
            Long categoriaId, Long tallaId, Long colorId) {
        var rows = kpiRepo.kpiSkuMensual(desde, hasta, canalId, temporadaId, categoriaId, tallaId, colorId);
        if (rows == null || rows.isEmpty()) return List.of();

        record Ym(int y, int m) {}
        record Key(Long id, int y, int m) {}

        Map<Ym, BigDecimal> totalIngresosMes = new HashMap<>();
        for (var r : rows) {
            Ym ym = new Ym(nz(r.getAnio()), nz(r.getMes()));
            totalIngresosMes.merge(ym, nzBD(r.getIngresos()), BigDecimal::add);
        }

        Map<Key, KpiSkuMesDTO> index = new HashMap<>();
        List<KpiSkuMesDTO> out = new ArrayList<>(rows.size());

        for (var r : rows) {
            int anio = nz(r.getAnio());
            int mes  = nz(r.getMes());
            Ym ym = new Ym(anio, mes);

            BigDecimal ingresos = nzBD(r.getIngresos());
            BigDecimal aporte = totalIngresosMes.getOrDefault(ym, BigDecimal.ZERO).signum() == 0
                    ? BigDecimal.ZERO
                    : ingresos.divide(totalIngresosMes.get(ym), 6, RoundingMode.HALF_UP);

            var dto = KpiSkuMesDTO.builder()
                    .anio(anio)
                    .mes(mes)
                    .skuId(r.getSkuId())
                    .sku(r.getSku())
                    .producto(r.getProducto())
                    .talla(r.getTalla())
                    .color(r.getColor())
                    .unidades(nzL(r.getUnidades()))
                    .ingresos(ingresos)
                    .aporteMes(aporte)
                    .build();

            out.add(dto);
            index.put(new Key(r.getSkuId(), anio, mes), dto);
        }

        for (var dto : out) {
            int y = dto.getAnio(), m = dto.getMes();
            Long id = dto.getSkuId();

            int prevY = y, prevM = m - 1;
            if (prevM == 0) { prevM = 12; prevY = y - 1; }

            var prev = index.get(new Key(id, prevY, prevM));
            if (prev != null) {
                dto.setVarMesAnteriorUnidades( varPct(dto.getUnidades(), prev.getUnidades()) );
                dto.setVarMesAnteriorIngresos( varPct(dto.getIngresos(), prev.getIngresos()) );
            }

            var yoy = index.get(new Key(id, y - 1, m));
            if (yoy != null) {
                dto.setVarYoYUnidades( varPct(dto.getUnidades(), yoy.getUnidades()) );
                dto.setVarYoYIngresos( varPct(dto.getIngresos(), yoy.getIngresos()) );
            }
        }

        ordenarKpi(out, KpiSkuMesDTO::getAporteMes);
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KpiCategoriaDTO> kpiPorCategoria(LocalDateTime desde, LocalDateTime hasta,
                                                Long canalId, Long temporadaId, Long categoriaId,
                                                Long tallaId, Long colorId) {
        // Actual
        var actual = kpiRepo.kpiCategoriaTotal(desde, hasta, canalId, temporadaId, categoriaId, tallaId, colorId);

        // Periodo anterior de igual duración
        var dur = Duration.between(desde, hasta);
        var desdePrev = desde.minus(dur);
        var hastaPrev = desde;
        var prev = kpiRepo.kpiCategoriaTotal(desdePrev, hastaPrev, canalId, temporadaId, categoriaId, tallaId, colorId);

        // Mismo periodo año anterior (YoY)
        var desdeYoY = desde.minusYears(1);
        var hastaYoY = hasta.minusYears(1);
        var yoy = kpiRepo.kpiCategoriaTotal(desdeYoY, hastaYoY, canalId, temporadaId, categoriaId, tallaId, colorId);

        // Total ingresos período actual para aporte
        var totalIngresos = actual.stream()
                .map(r -> nz(r.getIngresos()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Mapas para variaciones
        var prevMap = prev.stream().collect(Collectors.toMap(
                KpiAggCategoriaTotal::getCategoriaId,
                KpiAggCategoriaTotal::getIngresos));
        var yoyMap = yoy.stream().collect(Collectors.toMap(
                KpiAggCategoriaTotal::getCategoriaId,
                KpiAggCategoriaTotal::getIngresos));

        var out = new ArrayList<KpiCategoriaDTO>();
        for (var r : actual) {
            var ingresosAct = nz(r.getIngresos());
            var aporte = pct(ingresosAct, totalIngresos);

            var basePrev = nz(prevMap.get(r.getCategoriaId()));
            var varMes = growthPct(ingresosAct, basePrev);

            var baseYoy = nz(yoyMap.get(r.getCategoriaId()));
            var varYoy = growthPct(ingresosAct, baseYoy);

            out.add(KpiCategoriaDTO.builder()
                    .categoriaId(r.getCategoriaId())
                    .categoria(r.getCategoria())
                    .unidades(r.getUnidades() == null ? 0L : r.getUnidades())
                    .ingresos(ingresosAct)
                    .aportePct(aporte)
                    .variacionMesPct(varMes)
                    .variacionYoYPct(varYoy)
                    .build());
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KpiProductoDTO> kpiPorProducto(LocalDateTime desde, LocalDateTime hasta,
                                            Long canalId, Long temporadaId, Long categoriaId, Long tallaId, Long colorId) {
        var actual = kpiRepo.kpiProductoTotal(desde, hasta, canalId, temporadaId, categoriaId, tallaId, colorId);

        var dur = java.time.Duration.between(desde, hasta);
        var desdePrev = desde.minus(dur);
        var hastaPrev = desde;
        var prev = kpiRepo.kpiProductoTotal(desdePrev, hastaPrev, canalId, temporadaId, categoriaId, tallaId, colorId);

        var desdeYoY = desde.minusYears(1);
        var hastaYoY = hasta.minusYears(1);
        var yoy = kpiRepo.kpiProductoTotal(desdeYoY, hastaYoY, canalId, temporadaId, categoriaId, tallaId, colorId);

        var totalIngresos = actual.stream()
                .map(r -> nz(r.getIngresos()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var prevMap = prev.stream().collect(java.util.stream.Collectors.toMap(
                KpiAggProductoTotal::getProductoId,
                KpiAggProductoTotal::getIngresos));

        var yoyMap = yoy.stream().collect(java.util.stream.Collectors.toMap(
                KpiAggProductoTotal::getProductoId,
                KpiAggProductoTotal::getIngresos));

        var out = new java.util.ArrayList<KpiProductoDTO>();
        for (var r : actual) {
            var ingresosAct = nz(r.getIngresos());
            out.add(KpiProductoDTO.builder()
                    .productoId(r.getProductoId())
                    .producto(r.getProducto())
                    .unidades(r.getUnidades() == null ? 0L : r.getUnidades())
                    .ingresos(ingresosAct)
                    .aportePct(pct(ingresosAct, totalIngresos))
                    .variacionMesPct(growthPct(ingresosAct, nz(prevMap.get(r.getProductoId()))))
                    .variacionYoYPct(growthPct(ingresosAct, nz(yoyMap.get(r.getProductoId()))))
                    .build());
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KpiSkuDTO> kpiPorSku(LocalDateTime desde, LocalDateTime hasta,
                                    Long canalId, Long temporadaId, Long categoriaId, Long tallaId, Long colorId) {
        var actual = kpiRepo.kpiSkuTotal(desde, hasta, canalId, temporadaId, categoriaId, tallaId, colorId);

        var dur = java.time.Duration.between(desde, hasta);
        var desdePrev = desde.minus(dur);
        var hastaPrev = desde;
        var prev = kpiRepo.kpiSkuTotal(desdePrev, hastaPrev, canalId, temporadaId, categoriaId, tallaId, colorId);

        var desdeYoY = desde.minusYears(1);
        var hastaYoY = hasta.minusYears(1);
        var yoy = kpiRepo.kpiSkuTotal(desdeYoY, hastaYoY, canalId, temporadaId, categoriaId, tallaId, colorId);

        var totalIngresos = actual.stream()
                .map(r -> nz(r.getIngresos()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var prevMap = prev.stream().collect(Collectors.toMap(
                KpiAggSkuTotal::getSkuId,
                KpiAggSkuTotal::getIngresos));

        var yoyMap = yoy.stream().collect(Collectors.toMap(
                KpiAggSkuTotal::getSkuId,
                KpiAggSkuTotal::getIngresos));

        var out = new java.util.ArrayList<KpiSkuDTO>();
        for (var r : actual) {
            var ingresosAct = nz(r.getIngresos());
            out.add(KpiSkuDTO.builder()
                    .skuId(r.getSkuId())
                    .sku(r.getSku())
                    .producto(r.getProducto())
                    .talla(r.getTalla())
                    .color(r.getColor())
                    .unidades(r.getUnidades() == null ? 0L : r.getUnidades())
                    .ingresos(ingresosAct)
                    .aportePct(pct(ingresosAct, totalIngresos))
                    .variacionMesPct(growthPct(ingresosAct, nz(prevMap.get(r.getSkuId()))))
                    .variacionYoYPct(growthPct(ingresosAct, nz(yoyMap.get(r.getSkuId()))))
                    .build());
        }
        return out;
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

    private static int nz(Integer v) { 
        return v == null ? 0 : v; 
    }
    private static long nzL(Long v) { return v == null ? 0L : v; }
    private static BigDecimal nzBD(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static BigDecimal varPct(long actual, long previo) {
        if (previo == 0L) return null; // sin base
        return BigDecimal.valueOf(actual - previo)
                .divide(BigDecimal.valueOf(previo), 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal varPct(BigDecimal actual, BigDecimal previo) {
        if (previo == null || previo.signum() == 0) return null;
        return actual.subtract(previo)
                    .divide(previo, 6, RoundingMode.HALF_UP);
    }

    private <T> void ordenarKpi(List<T> list, Function<T, BigDecimal> aporteGetter) {
        list.sort(Comparator
            .comparing((T x) -> {
                try {
                    var anio = (Integer)x.getClass().getMethod("getAnio").invoke(x);
                    return anio;
                } catch (Exception e) { return 0; }
            })
            .thenComparing((T x) -> {
                try {
                    var mes = (Integer)x.getClass().getMethod("getMes").invoke(x);
                    return mes;
                } catch (Exception e) { return 0; }
            })
            .thenComparing(aporteGetter, Comparator.nullsLast(Comparator.reverseOrder()))
        );
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
    private static BigDecimal pct(BigDecimal num, BigDecimal den) {
        if (den == null || den.compareTo(BigDecimal.ZERO) == 0) return null;
        return num.multiply(BigDecimal.valueOf(100))
                .divide(den, 2, RoundingMode.HALF_UP);
    }
    private static BigDecimal growthPct(BigDecimal actual, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) return null;
        return actual.subtract(base)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(base, 2, RoundingMode.HALF_UP);
    }
}
