package com.colors.savd.model;

import java.time.LocalDateTime;
import java.util.List;

import com.colors.savd.model.enums.EstatusRegistro;

import jakarta.persistence.*;
import lombok.*;

@Entity 
@Table(name = "producto", uniqueConstraints = {
  @UniqueConstraint(name = "uq_producto__nombre_categoria", columnNames = {"nombre", "categoria_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Producto {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "categoria_id", foreignKey = @ForeignKey(name = "fk_producto__categoria"))
  private Categoria categoria;

  @Column(nullable = false, length = 160)
  private String nombre;

  @Column(length = 255)
  private String descripcion;

  @Enumerated(EnumType.STRING)
  @Column(name = "estatus_registro", nullable = false, length = 12)
  private EstatusRegistro estatusRegistro = EstatusRegistro.VISIBLE;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "producto", fetch = FetchType.LAZY)
  private List<VarianteSku> variantes;

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
