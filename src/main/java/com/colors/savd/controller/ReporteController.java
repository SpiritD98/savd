package com.colors.savd.controller;

import com.colors.savd.dto.TopProductoDTO;
import com.colors.savd.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
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
}

