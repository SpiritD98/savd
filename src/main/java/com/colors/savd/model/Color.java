package com.colors.savd.model;

import java.time.LocalDateTime;
import java.util.List;

import com.colors.savd.model.enums.EstatusRegistro;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "color")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Color {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20, unique = true)
  private String codigo; // ej. BLK, WHT

  @Column(nullable = false, length = 60)
  private String nombre; // ej. Negro, Blanco

  @Column(length = 7)
  private String hex; // ej. #000000

  @Enumerated(EnumType.STRING)
  @Column(name = "estatus_registro", nullable = false, length = 12)
  private EstatusRegistro estatusRegistro = EstatusRegistro.VISIBLE;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // Relación 1-N con alias (opcional de mapear al inicio)
  @OneToMany(mappedBy = "color", fetch = FetchType.LAZY)
  private List<ColorAlias> aliases;

  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
