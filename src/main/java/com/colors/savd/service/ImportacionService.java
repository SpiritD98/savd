package com.colors.savd.service;

import java.io.InputStream;

import com.colors.savd.dto.ImportOpcionesDTO;
import com.colors.savd.dto.ImportResultadoDTO;

public interface ImportacionService {
    /**
     * Importa ventas desde un archivo Excel.
     * @param in stream del archivo Excel
     * @param nombreArchivo nombre recibido (para bitacora)
     * @param usuarioId quien ejecuta la importacion
     * @param opciones opciones de importacion (modo de temporada, temporada fija, observacion, etc.)
     * @return resumen de filas OK/ERROR y bitacora creada
     */
    ImportResultadoDTO importarVentasExcel(InputStream in, String nombreArchivo, Long usuarioId, ImportOpcionesDTO opciones);

    default ImportResultadoDTO importarVentasExcel(InputStream in, String nombreArchivo, Long usuarioId){
        return importarVentasExcel(in, nombreArchivo, usuarioId, new ImportOpcionesDTO());
    }
}
