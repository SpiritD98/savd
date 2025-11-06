package com.colors.savd.model;

import java.time.LocalDateTime;

import com.colors.savd.model.enums.EstatusRegistro;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parametro_reposicion", uniqueConstraints = {
  @UniqueConstraint(name = "uq_parametro_repo__sku", columnNames = "sku_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ParametroReposicion {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Relación 1–a–1 con VarianteSku (obligatoria)
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sku_id", unique = true, foreignKey = @ForeignKey(name = "fk_parametro_repo__sku"))
  private VarianteSku sku;

  @Column(name = "min_stock", nullable = false)
  private Integer minStock = 0;

  @Column(name = "lead_time_dias", nullable = false)
  private Integer leadTimeDias = 0;

  @Column(name = "stock_seguridad", nullable = false)
  private Integer stockSeguridad = 0;

  @Column(name = "ultima_actualizacion", nullable = false)
  private LocalDateTime ultimaActualizacion;

  @Enumerated(EnumType.STRING)
  @Column(name = "estatus_registro", nullable = false, length = 12)
  private EstatusRegistro estatusRegistro = EstatusRegistro.VISIBLE;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  void prePersist() {
    var now = LocalDateTime.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
    if (ultimaActualizacion == null) ultimaActualizacion = now;
  }
  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
    ultimaActualizacion = LocalDateTime.now();
  }
}

