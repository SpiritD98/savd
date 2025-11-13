package com.colors.savd.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.colors.savd.dto.MovimientoInventarioDTO;
import com.colors.savd.service.InventarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    /**
     * Registra stock inicial para un SKU.
     * Roles: ADMIN o ANALISTA.
     */
    @PreAuthorize("hasAnyRole('ADMIN','ANALISTA')")
    @PostMapping(path = "/inicial", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> stockInicial(
            @Valid @RequestBody MovimientoInventarioDTO dto,
            @RequestParam("usuarioId") Long usuarioId) {
        Long id = inventarioService.registrarStockInicial(dto, usuarioId);
        return ResponseEntity.ok(id);
    }

    /**
     * Registra un ingreso de inventario (compra, ajuste positivo, etc).
     * Roles: ADMIN o ANALISTA.
     */
    @PreAuthorize("hasAnyRole('ADMIN','ANALISTA')")
    @PostMapping(path = "/ingresos", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> ingreso(
            @Valid @RequestBody MovimientoInventarioDTO dto,
            @RequestParam("usuarioId") Long usuarioId) {
        Long id = inventarioService.registrarIngreso(dto, usuarioId);
        return ResponseEntity.ok(id);
    }
}
