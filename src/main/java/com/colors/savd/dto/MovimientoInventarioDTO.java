package com.colors.savd.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoInventarioDTO {
    // Si no envían, usamos now() en el service
    private LocalDateTime fechaHora;

    @NotNull(message = "skuId es obligatorio")
    private Long skuId;

    @NotNull @Min(value = 1, message = "La cantidad debe ser > 0")
    private Integer cantidad;

    // Opcionales pero recomendados
    private String referencia;   // ej. “DOC-123”
    private String observacion;  // ej. “Ingreso por compra #OC-999”

    // Opcional: canal (si aplica a INGRESOS operativos)
    private Long canalId;
}
