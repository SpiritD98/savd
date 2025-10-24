package com.colors.savd.exception;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Representa un error en la API REST.
 */

@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ApiError {
    private Instant timestamp;  //Instante del error
    private int status;         //Codigo HTTP (ej. 400)
    private String error;       //texto del estado (ej. "Bad Request")
    private String mensaje;     //Detalle legible
    private String path;        //Endpoint (URI)
    private String code;        //Opcional: código de negocio (ej. "VALIDATION", "BUSINESS")
}
