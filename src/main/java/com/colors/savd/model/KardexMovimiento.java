package com.colors.savd.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kardex_movimiento", uniqueConstraints = {
  @UniqueConstraint(name = "uq_kardex__idempotency", columnNames = "idempotency_key")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class KardexMovimiento {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "fecha_hora", nullable = false)
  private LocalDateTime fechaHora;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sku_id", foreignKey = @ForeignKey(name = "fk_kardex__sku"))
  private VarianteSku sku;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tipo_mov_id", foreignKey = @ForeignKey(name = "fk_kardex__tipo"))
  private TipoMovimiento tipo;

  @Column(nullable = false)
  private Integer cantidad;

  // CHECK (-1,1) en BD; en app validas antes de persistir
  @Column(nullable = false)
  private Integer signo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "canal_id", foreignKey = @ForeignKey(name = "fk_kardex__canal"))
  private CanalVenta canal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venta_id", foreignKey = @ForeignKey(name = "fk_kardex__venta"))
  private Venta venta;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venta_detalle_id", foreignKey = @ForeignKey(name = "fk_kardex__venta_detalle"))
  private VentaDetalle ventaDetalle;

  @Column(length = 160)
  private String referencia;

  @Column(length = 255)
  private String observacion;

  @Column(name = "idempotency_key", length = 180)
  private String idempotencyKey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_kardex__usuario"))
  private Usuario usuario;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
