package com.colors.savd.model;

import com.colors.savd.model.enums.EstatusRegistro;
import com.colors.savd.model.enums.EstadoTemporada;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "temporada")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Temporada {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tipo_temp_id", foreignKey = @ForeignKey(name = "fk_temporada__tipo_temporada"))
  private TipoTemporada tipo;

  @Column(nullable = false, length = 160)
  private String nombre;

  @Column(name = "fecha_inicio", nullable = false)
  private LocalDate fechaInicio;

  @Column(name = "fecha_fin", nullable = false)
  private LocalDate fechaFin;

  @Column(nullable = false)
  private Integer prioridad = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_negocio", nullable = false, length = 8)
  private EstadoTemporada estadoNegocio = EstadoTemporada.ACTIVA; // ACTIVA/CERRADA

  @Column
  private Integer anio;

  @Column(length = 255)
  private String descripcion;

  @Enumerated(EnumType.STRING)
  @Column(name = "estatus_registro", nullable = false, length = 12)
  private EstatusRegistro estatusRegistro = EstatusRegistro.VISIBLE;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
