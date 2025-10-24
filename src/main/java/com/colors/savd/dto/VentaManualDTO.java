//Se añadio el DTO Venta Manual
package com.colors.savd.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaManualDTO {

  @NotNull(message = "La fecha/hora es obligatoria")
  private LocalDateTime fechaHora;

  @NotNull(message = "El canal es obligatorio")
  private Long canalId;

  // opcional
  private Long temporadaId;

  // obligatorio si canal=FISICO (validar en el servicio)
  @Size(max = 120, message = "La referencia no debe exceder 120 caracteres")
  private String referenciaOrigen;

  @NotEmpty(message = "Debe registrar al menos un item")
  private List<Item> items;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Item {
    @NotNull(message = "skuId es obligatorio")
    private Long skuId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.00", inclusive = true, message = "El precio no puede ser negativo")
    private BigDecimal precioUnitario;
  }
}
