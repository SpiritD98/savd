package com.colors.savd.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.colors.savd.dto.ImportOpcionesDTO;
import com.colors.savd.dto.ImportResultadoDTO;
import com.colors.savd.service.ImportacionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/importaciones")
@RequiredArgsConstructor
public class ImportacionController {

    private final ImportacionService importacionService;
    /**
     * Sube un Excel de ventas (XLSX) y lo procesa agrupando por cabecera.
     * @param file archivo Excel
     * @param usuarioId responsable de la carga (por ahora como param; en prod vendrá del token)
    */

    @PostMapping(path = "/ventas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ImportResultadoDTO importarVentasExcel(
        @RequestPart("file") MultipartFile file, 
        @RequestParam("usuarioId") Long usuarioId,
        @RequestPart(name = "opciones", required = false) ImportOpcionesDTO opciones
    ) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debe adjuntar un archivo Excel (.xlsx).");
        }
        String nombre = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "ventas.xlsx";
        try(var in = file.getInputStream()){
            return importacionService.importarVentasExcel(in, nombre, usuarioId, opciones);
        }
    }
}
