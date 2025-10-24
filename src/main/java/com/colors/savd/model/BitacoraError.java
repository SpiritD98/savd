package com.colors.savd.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bitacora_error")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BitacoraError {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "bitacora_id", foreignKey = @ForeignKey(name = "fk_bitacora_error__bitacora"))
  private BitacoraCarga bitacora;

  @Column(name = "fila_origen")
  private Integer filaOrigen;

  @Column(nullable = false, length = 80)
  private String campo;

  @Column(name = "mensaje_error", nullable = false, length = 255)
  private String mensajeError;

  @Column(name = "valor_original", length = 255)
  private String valorOriginal;

  @Column(name = "fecha_hora_registro", nullable = false)
  private LocalDateTime fechaHoraRegistro;
}

