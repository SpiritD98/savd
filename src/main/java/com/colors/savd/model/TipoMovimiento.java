package com.colors.savd.model;

import java.time.LocalDateTime;

import com.colors.savd.model.enums.EstatusRegistro;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipo_movimiento", uniqueConstraints = {
  @UniqueConstraint(name = "uq_tipo_movimiento__codigo", columnNames = "codigo")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TipoMovimiento {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 40)
  private String codigo; // INICIAL, INGRESO, VENTA, AJUSTE, ANULACION

  @Column(nullable = false, length = 100)
  private String nombre;

  // CHECK (-1, 1) en BD; en app valida antes de persistir
  @Column(name = "signo_default", nullable = false)
  private Integer signoDefault;

  @Enumerated(EnumType.STRING)
  @Column(name = "estatus_registro", nullable = false, length = 12)
  private EstatusRegistro estatusRegistro = EstatusRegistro.VISIBLE;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}

