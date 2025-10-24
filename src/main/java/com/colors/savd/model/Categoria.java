package com.colors.savd.model;

import java.time.LocalDateTime;
import java.util.List;

import com.colors.savd.model.enums.EstatusRegistro;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categoria", uniqueConstraints = {
  @UniqueConstraint(name = "uq_categoria__nombre", columnNames = "nombre")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Categoria {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
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

  @OneToMany(mappedBy = "categoria", fetch = FetchType.LAZY)
  private List<Producto> productos;
}
