package com.colors.savd.dto;

import java.time.LocalDateTime;

import com.colors.savd.model.enums.TipoCarga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BitacoraResumenDTO {
    private Long id;
    private LocalDateTime fechaHora;
    private String usuarioEmail;
    private TipoCarga tipoCarga;
    private String archivoNombre;
    private Integer filasOk;
    private Integer filasError;
    private String rutaLog; // puede ser null si no se seteó
}
