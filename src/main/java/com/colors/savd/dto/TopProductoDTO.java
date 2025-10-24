// Se creó el DTO Top producto
package com.colors.savd.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopProductoDTO {
  private Long skuId;
  private String sku;
  private String producto;
  private String talla;
  private String color;
  private Long   unidades;
  private BigDecimal ingresos; // total S/.
}
