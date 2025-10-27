package com.colors.savd.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class LineaVentaDTO {
    @NotNull(message = "skuId es obligatorio")
    private Long skuId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.00", inclusive = true, message = "El precio no puede ser negativo")
    private BigDecimal precioUnitario;

    // Opcional: si no se envía, usar precio_lista de VarianteSku
    @DecimalMin(value = "0.00", inclusive = true, message = "El precio lista no puede ser negativo")
    private BigDecimal precioLista;
}
