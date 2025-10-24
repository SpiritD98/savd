package com.colors.savd.model;

import java.time.LocalDateTime;
import java.util.List;

import com.colors.savd.model.enums.EstatusRegistro;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "talla")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Talla {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20, unique = true)
  private String codigo; // ej. S, M, 30

  @Column(nullable = false, length = 60)
  private String nombre; // ej. Small, Medium

  @Column(nullable = false)
  private Integer orden;

  @Enumerated(EnumType.STRING)
  @Column(name = "estatus_registro", nullable = false, length = 12)
  private EstatusRegistro estatusRegistro = EstatusRegistro.VISIBLE;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // Relación 1-N con alias (opcional de mapear al inicio)
  @OneToMany(mappedBy = "talla", fetch = FetchType.LAZY)
  private List<TallaAlias> aliases;
}
