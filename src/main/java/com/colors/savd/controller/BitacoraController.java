package com.colors.savd.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.colors.savd.dto.BitacoraErrorDTO;
import com.colors.savd.dto.BitacoraResumenDTO;
import com.colors.savd.model.BitacoraCarga;
import com.colors.savd.model.BitacoraError;
import com.colors.savd.model.enums.TipoCarga;
import com.colors.savd.repository.BitacoraCargaRepository;
import com.colors.savd.repository.BitacoraErrorRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bitacoras")
@RequiredArgsConstructor
public class BitacoraController {
    private final BitacoraCargaRepository bitacoraRepo;
    private final BitacoraErrorRepository bitErrorRepo;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<BitacoraResumenDTO> listar(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) TipoCarga tipo
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaHora"));
        Page<BitacoraCarga> src = (tipo == null) ? bitacoraRepo.findAllByOrderByFechaHoraDesc(pageable) : bitacoraRepo.findByTipoCargaOrderByFechaHoraDesc(tipo, pageable);
        
        return src.map(b -> BitacoraResumenDTO.builder()
        .id(b.getId())
        .fechaHora(b.getFechaHora())
        .usuarioEmail(b.getUsuario() != null ? b.getUsuario().getEmail() : null)
        .tipoCarga(b.getTipoCarga())
        .archivoNombre(b.getArchivoNombre())
        .filasOk(b.getFilasOk())
        .filasError(b.getFilasError())
        .rutaLog(b.getRutaLog())
        .build());
    }
    // GET /api/bitacoras/{id}
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public BitacoraResumenDTO obtener(@PathVariable Long id) {
        BitacoraCarga b = bitacoraRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bitácora no encontrada"));
        return BitacoraResumenDTO.builder()
            .id(b.getId())
            .fechaHora(b.getFechaHora())
            .usuarioEmail(b.getUsuario() != null ? b.getUsuario().getEmail() : null)
            .tipoCarga(b.getTipoCarga())
            .archivoNombre(b.getArchivoNombre())
            .filasOk(b.getFilasOk())
            .filasError(b.getFilasError())
            .rutaLog(b.getRutaLog())
        .build();
    }

    // GET /api/bitacoras/{id}/errores?page=0&size=50
    @GetMapping(path = "/{id}/errores", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<BitacoraErrorDTO> errores(@PathVariable Long id,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
    // valida existencia
    bitacoraRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bitácora no encontrada"));

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "filaOrigen"));
    Page<BitacoraError> src = bitErrorRepo.findByBitacora_IdOrderByFilaOrigenAsc(id, pageable);

    return src.map(e -> BitacoraErrorDTO.builder()
        .id(e.getId())
        .filaOrigen(e.getFilaOrigen())
        .campo(e.getCampo())
        .mensajeError(e.getMensajeError())
        .valorOriginal(e.getValorOriginal())
        .fechaHoraRegistro(e.getFechaHoraRegistro())
        .build());
    }

    // GET /api/bitacoras/{id}/log  (Solo ADMIN por tu SecurityConfig)
    @GetMapping(path = "/{id}/log")
    public ResponseEntity<ByteArrayResource> descargarLog(@PathVariable Long id) {
        BitacoraCarga b = bitacoraRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bitácora no encontrada"));

        String ruta = b.getRutaLog();
        if (StringUtils.isBlank(ruta)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bitácora sin ruta de log asociada.");
        }

        File f = new File(ruta);
        if (!f.exists() || !f.isFile()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo de log no encontrado: " + ruta);
        }

        try {
        byte[] bytes = Files.readAllBytes(Path.of(ruta));
        ByteArrayResource resource = new ByteArrayResource(bytes);

        String filename = f.getName();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        // content-type: si es .gz muchos navegadores lo descargan igual
        MediaType ct = filename.endsWith(".gz")
            ? MediaType.APPLICATION_OCTET_STREAM
            : MediaType.TEXT_PLAIN;

        return ResponseEntity.ok()
            .headers(headers)
            .contentType(ct)
            .contentLength(bytes.length)
            .body(resource);

        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el log", ex);
        }
    }
}
