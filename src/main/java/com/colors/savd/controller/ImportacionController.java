package com.colors.savd.controller;

import org.springframework.web.multipart.MultipartFile;

import com.colors.savd.dto.ImportOpcionesDTO;
import com.colors.savd.dto.ImportResultadoDTO;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.service.ImportacionService;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/importaciones")
@RequiredArgsConstructor
@Validated
public class ImportacionController {

    private final ImportacionService importacionService;
    /**
     * Sube un Excel de ventas (XLSX) y lo procesa agrupando por cabecera.
     * @param file archivo Excel
     * @param usuarioId responsable de la carga (por ahora como param; en prod vendrá del token)
    */
    @PreAuthorize("hasAnyRole('ADMIN','ANALISTA')")
    @PostMapping(path="/ventas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ImportResultadoDTO importarVentasExcel(
        @RequestPart("file") MultipartFile file, 
        @RequestParam("usuarioId") Long usuarioId,
        @RequestPart(name = "opciones", required = false) ImportOpcionesDTO opciones
    ) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Debe adjuntar un archivo Excel (.xlsx).");
        }
        if (usuarioId == null) {
            throw new BusinessException("usuarioId es obligatorio.");
        }
        if (opciones == null){
            opciones = ImportOpcionesDTO.builder().build();
        } 

        String nombre = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "ventas.xlsx";
        
        try(InputStream in = file.getInputStream()){
            return importacionService.importarVentasExcel(in, nombre, usuarioId, opciones);
        } catch (IOException e) {
            // Lo dejamos subir como Runtime para que GlobalExceptionHandler lo pinte como 500
            throw new RuntimeException("Error leyendo el archivo Excel", e);
        }
    }
}
