package com.colors.savd.controller;

import com.colors.savd.dto.KpiCategoriaDTO;
import com.colors.savd.dto.KpiProductoMesDTO;
import com.colors.savd.dto.KpiSkuMesDTO;
import com.colors.savd.dto.TopProductoDTO;
import com.colors.savd.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Validated
public class ReporteController {

  private final ReporteService reporteService;

  /**
   * Top-15 de productos más vendidos en un rango (JSON).
   * Params: desde, hasta (ISO), canalId opcional.
   */
  @GetMapping(path = "/top15", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<TopProductoDTO> top15(
      @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
      @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
      @RequestParam(value = "canalId", required = false) Long canalId
  ) {
    // Validación de rango se hace en ReporteServiceImpl (BusinessException si algo está mal)
    return reporteService.top15(desde, hasta, canalId);
  }

  /**
   * Descarga del Reporte Ejecutivo (XLSX) con 2 hojas: Top15 y Alertas.
   * Params: desde, hasta (ISO), canalId opcional.
   */
  @GetMapping(path = "/ejecutivo.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public ResponseEntity<byte[]> descargarReporteEjecutivo(
      @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
      @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
      @RequestParam(value = "canalId", required = false) Long canalId
  ) {
    byte[] xlsx = reporteService.exportarReporteEjecutivo(desde, hasta, canalId);

    String fn = "reporte_ejecutivo_" +
        desde.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + "_" +
        hasta.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".xlsx";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.setContentDisposition(ContentDisposition.attachment().filename(fn).build());
    headers.setCacheControl(CacheControl.noCache().getHeaderValue());

    return new ResponseEntity<>(xlsx, headers, HttpStatus.OK);
  }

  @GetMapping(path = "/kpis/producto", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<KpiProductoMesDTO> kpiProducto(
      @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
      @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
      @RequestParam(value = "canalId", required = false) Long canalId,
      @RequestParam(value = "temporadaId", required = false) Long temporadaId,
      @RequestParam(value = "categoriaId", required = false) Long categoriaId,
      @RequestParam(value = "tallaId", required = false) Long tallaId,
      @RequestParam(value = "colorId", required = false) Long colorId
  ) {
      return reporteService.kpiProductoMensual(desde, hasta, canalId, temporadaId, categoriaId, tallaId, colorId);
  }

  @GetMapping(path = "/kpis/sku", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<KpiSkuMesDTO> kpiSku(
      @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
      @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
      @RequestParam(value = "canalId", required = false) Long canalId,
      @RequestParam(value = "temporadaId", required = false) Long temporadaId,
      @RequestParam(value = "categoriaId", required = false) Long categoriaId,
      @RequestParam(value = "tallaId", required = false) Long tallaId,
      @RequestParam(value = "colorId", required = false) Long colorId
  ) {
      return reporteService.kpiSkuMensual(desde, hasta, canalId, temporadaId, categoriaId, tallaId, colorId);
  }

  @GetMapping(path = "/kpis/categoria", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<KpiCategoriaDTO> kpisCategoria(
        @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
        @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
        @RequestParam(value = "canalId",     required = false) Long canalId,
        @RequestParam(value = "temporadaId", required = false) Long temporadaId,
        @RequestParam(value = "categoriaId", required = false) Long categoriaId,
        @RequestParam(value = "tallaId",     required = false) Long tallaId,
        @RequestParam(value = "colorId",     required = false) Long colorId
  ){
    return reporteService.kpiPorCategoria(desde, hasta, canalId, temporadaId, categoriaId, tallaId, colorId);
  }
}

