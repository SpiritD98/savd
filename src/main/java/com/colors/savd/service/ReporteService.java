package com.colors.savd.service;

import java.time.LocalDateTime;
import java.util.List;

import com.colors.savd.dto.AlertaStockDTO;
import com.colors.savd.dto.KpiCategoriaDTO;
import com.colors.savd.dto.KpiCategoriaMesDTO;
import com.colors.savd.dto.KpiProductoDTO;
import com.colors.savd.dto.KpiProductoMesDTO;
import com.colors.savd.dto.KpiSkuDTO;
import com.colors.savd.dto.KpiSkuMesDTO;
import com.colors.savd.dto.TopProductoDTO;

public interface ReporteService {
    List<TopProductoDTO> top15(LocalDateTime desde, LocalDateTime hasta, Long canalId);
    List<AlertaStockDTO> alertasStock(LocalDateTime corte); //si corte == null, usar now
    byte[] exportarReporteEjecutivo(LocalDateTime desde, LocalDateTime hasta, Long canalId);
    List<KpiCategoriaMesDTO> kpiCategoriaMensual(LocalDateTime desde, LocalDateTime hasta, Long canalId, Long temporadaId, Long categoriaId, Long tallaId, Long colorId);
    List<KpiProductoMesDTO> kpiProductoMensual(LocalDateTime desde, LocalDateTime hasta, Long canalId, Long temporadaId, Long categoriaId, Long tallaId, Long colorId);
    List<KpiSkuMesDTO> kpiSkuMensual(LocalDateTime desde, LocalDateTime hasta, Long canalId, Long temporadaId, Long categoriaId, Long tallaId, Long colorId);
    List<KpiCategoriaDTO> kpiPorCategoria(LocalDateTime desde, LocalDateTime hasta, Long canalId, Long temporadaId, Long categoriaId, Long tallaId, Long colorId);
    List<KpiProductoDTO> kpiPorProducto(LocalDateTime desde, LocalDateTime hasta, Long canalId, Long temporadaId, Long categoriaId, Long tallaId, Long colorId);
    List<KpiSkuDTO> kpiPorSku(LocalDateTime desde, LocalDateTime hasta, Long canalId, Long temporadaId, Long categoriaId, Long tallaId, Long colorId);
}
