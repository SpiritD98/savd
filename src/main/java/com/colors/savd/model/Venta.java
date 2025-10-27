package com.colors.savd.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.colors.savd.model.enums.EstadoVenta;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="venta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Venta {
  
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name="fecha_hora", nullable=false)
  private LocalDateTime fechaHora;

  @ManyToOne(fetch=FetchType.LAZY, optional=false)
  @JoinColumn(name="canal_id", foreignKey=@ForeignKey(name="fk_venta__canal"))
  private CanalVenta canal;

  @Column(name="referencia_origen", length=120)
  private String referenciaOrigen;

  @ManyToOne(fetch=FetchType.LAZY)
  @JoinColumn(name="temporada_id", foreignKey=@ForeignKey(name="fk_venta__temporada"))
  private Temporada temporada;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false, length = 7)
  private EstadoVenta estado = EstadoVenta.ACTIVA;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total = BigDecimal.ZERO;

  // auditoría
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by", foreignKey=@ForeignKey(name="fk_venta__created_by"))
  private Usuario createdBy;

  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="updated_by", foreignKey=@ForeignKey(name="fk_venta__updated_by"))
  private Usuario updatedBy;

  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="anulada_por", foreignKey=@ForeignKey(name="fk_venta__anulada_por"))
  private Usuario anuladaPor;
  
  @Column(name="anulada_at")
  private LocalDateTime anuladaAt;

  @Column(name="created_at", nullable=false)
  private LocalDateTime createdAt;

  @Column(name="updated_at", nullable=false)
  private LocalDateTime updatedAt;

  @PrePersist
  void prePersist(){
    LocalDateTime now = LocalDateTime.now();
    if(createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
    if (fechaHora == null) fechaHora = now;
  }

  @PreUpdate
  void preUpdate(){
    updatedAt = LocalDateTime.now();
  }
}
