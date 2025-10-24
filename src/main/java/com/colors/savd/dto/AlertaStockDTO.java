//Se subieron las clases DTO
package com.colors.savd.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaStockDTO {
  private Long skuId;
  private String sku;
  private Integer stockActual;
  private Integer minStock;
  private Integer coberturaDias; // días estimados de cobertura
  private Integer rop;           // Reorder Point (punto de pedido)
  private String semaforo;       // ROJO/AMARILLO/VERDE
}
