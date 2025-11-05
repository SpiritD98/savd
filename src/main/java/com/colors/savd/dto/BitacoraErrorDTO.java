package com.colors.savd.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BitacoraErrorDTO {
  private Long id;
  private Integer filaOrigen;
  private String campo;
  private String mensajeError;
  private String valorOriginal; // puede venir null si no lo almacenas
  private LocalDateTime fechaHoraRegistro;
}
