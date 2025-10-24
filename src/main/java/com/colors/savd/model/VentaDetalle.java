package com.colors.savd.model;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "venta_detalle")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VentaDetalle {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "venta_id", foreignKey = @ForeignKey(name = "fk_venta_detalle__venta"))
  private Venta venta;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sku_id", foreignKey = @ForeignKey(name = "fk_venta_detalle__sku"))
  private VarianteSku sku;

  @Column(nullable = false)
  private Integer cantidad;

  @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
  private BigDecimal precioUnitario;

  @Column(name = "precio_lista", nullable = false, precision = 10, scale = 2)
  private BigDecimal precioLista;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal importe;
}

