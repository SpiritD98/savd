package com.colors.savd.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaManualDTO {

  @NotNull(message = "La fecha/hora es obligatoria")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime fechaHora;

  @NotNull(message = "El canal es obligatorio")
  private Long canalId;

  // opcional (si no se envía, el backend puede asignar automáticamente según la fecha)
  private Long temporadaId;

  // obligatorio si canal = FISICO (validar en el servicio)
  @Size(max = 120, message = "La referencia no debe exceder 120 caracteres")
  private String referenciaOrigen;

  @NotEmpty(message = "Debe registrar al menos un item")
  @Valid
  private List<LineaVentaDTO> items;
}
