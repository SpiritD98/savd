package com.colors.savd.model;

import java.time.LocalDateTime;

import com.colors.savd.model.enums.EstatusRegistro;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipo_temporada", uniqueConstraints = {
  @UniqueConstraint(name = "uq_tipo_temporada__codigo", columnNames = "codigo")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TipoTemporada {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 40)
  private String codigo; // TEMPORADA, DROP, CAMPANIA

  @Column(nullable = false, length = 100)
  private String nombre;

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
  }
  @PreUpdate
  void preUpdate() { updatedAt = LocalDateTime.now(); }
}

