package com.colors.savd.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResultadoDTO {
  private Long bitacoraId;
  private Integer filasOk;
  private Integer filasError;
  private List<String> erroresMuestra; // primeros N errores para mostrar en UI
}
