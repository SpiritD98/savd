package com.colors.savd.controller;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import com.colors.savd.dto.LineaVentaRespDTO;
import com.colors.savd.dto.VentaManualDTO;
import com.colors.savd.dto.VentaRespuestaDTO;
import com.colors.savd.model.VarianteSku;
import com.colors.savd.model.Venta;
import com.colors.savd.model.VentaDetalle;
import com.colors.savd.model.enums.EstadoVenta;
import com.colors.savd.repository.VentaDetalleRepository;
import com.colors.savd.repository.VentaRepository;
import com.colors.savd.service.VentaService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@Slf4j
@Validated
public class VentaController {

    private final VentaService ventaService;
    private final VentaRepository ventaRepo;
    private final VentaDetalleRepository ventaDetRepo;

    /**
     * Crea una venta manual.
     * - Body: VentaManualDTO (JSON)
     * - Query param: usuarioId
     * Respuesta: 201 Created + VentaRespuestaDTO
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VentaRespuestaDTO> crearVentaManual(
            @Valid @RequestBody VentaManualDTO dto,
            @RequestParam("usuarioId") Long usuarioId,
            UriComponentsBuilder uriBuilder) {

        Long ventaId = ventaService.registrarVentaManual(dto, usuarioId);

        // Construimos DTO de respuesta
        VentaRespuestaDTO resp = buildVentaRespuesta(ventaId);

        var location = uriBuilder
                .path("/api/ventas/{id}")
                .buildAndExpand(ventaId)
                .toUri();

        return ResponseEntity
                .created(location)
                .body(resp);
    }

    /**
     * Anula una venta.
     * - Path: /{ventaId}/anular
     * - Query params: usuarioId, motivo (opcional pero recomendado)
     * Respuesta: 200 OK + VentaRespuestaDTO (ya anulada)
     */
    @PostMapping(path = "/{ventaId}/anular", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VentaRespuestaDTO> anularVenta(
            @PathVariable("ventaId") Long ventaId,
            @RequestParam("usuarioId") Long usuarioId,
            @RequestParam(value = "motivo", required = false, defaultValue = "Anulación solicitada") String motivo) {

        ventaService.anularVenta(ventaId, usuarioId, motivo);

        // Devolver estado final
        VentaRespuestaDTO resp = buildVentaRespuesta(ventaId);
        return ResponseEntity.ok(resp);
    }

    /**
     * Obtiene una venta por id (para UI o verificación).
     */
    @GetMapping(path = "/{ventaId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<VentaRespuestaDTO> obtenerVenta(@PathVariable("ventaId") Long ventaId) {
        VentaRespuestaDTO resp = buildVentaRespuesta(ventaId);
        return ResponseEntity.ok(resp);
    }

    // ===================== Helpers =====================

    /**
     * Carga venta + detalles y arma VentaRespuestaDTO.
     * Se ejecuta dentro de transacción para evitar Lazy issues.
     */
    private VentaRespuestaDTO buildVentaRespuesta(Long ventaId) {
        Venta v = ventaRepo.findById(ventaId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada id=" + ventaId));

        List<VentaDetalle> detalles = ventaDetRepo.findByVenta_Id(ventaId);

        VentaRespuestaDTO.VentaRespuestaDTOBuilder b = VentaRespuestaDTO.builder()
                .ventaId(v.getId())
                .fechaHora(v.getFechaHora())
                .canalCodigo(v.getCanal() != null ? v.getCanal().getCodigo() : null)
                .referenciaOrigen(v.getReferenciaOrigen())
                .estado(v.getEstado() != null ? v.getEstado().name() : EstadoVenta.ACTIVA.name())
                .total(v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO);

        List<LineaVentaRespDTO> lineas = new ArrayList<>(detalles.size());
        for (VentaDetalle d : detalles) {
            VarianteSku sku = d.getSku();
            String skuTxt    = (sku != null) ? safe(sku.getSku()) : "";
            String prodTxt   = (sku != null && sku.getProducto() != null) ? safe(sku.getProducto().getNombre()) : "";
            String tallaTxt  = (sku != null && sku.getTalla() != null) ? safe(sku.getTalla().getCodigo()) : "";
            String colorTxt  = (sku != null && sku.getColor() != null) ? safe(sku.getColor().getNombre()) : "";

            lineas.add(LineaVentaRespDTO.builder()
                    .skuId(sku != null ? sku.getId() : null)
                    .sku(skuTxt)
                    .producto(prodTxt)
                    .talla(tallaTxt)
                    .color(colorTxt)
                    .cantidad(d.getCantidad())
                    .precioUnitario(d.getPrecioUnitario())
                    .importe(d.getImporte())
                    .build());
        }

        return b.detalles(lineas).build();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String fmtFecha(java.time.LocalDateTime dt) {
        return dt == null ? "" : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}