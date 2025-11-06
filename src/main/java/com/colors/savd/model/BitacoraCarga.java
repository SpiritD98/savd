package com.colors.savd.model;

import java.time.LocalDateTime;

import com.colors.savd.model.enums.TipoCarga;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "bitacora_carga")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BitacoraCarga {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "fecha_hora", nullable = false)
  private LocalDateTime fechaHora;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_bitacora__usuario"))
  private Usuario usuario;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_carga", nullable = false, length = 20)
  private TipoCarga tipoCarga; // VENTAS, INGRESOS, INICIAL, PARAMETROS, CATALOGO, RECLASIFICACION

  @Column(name = "archivo_nombre", length = 180)
  private String archivoNombre;

  @Column(name = "filas_ok", nullable = false)
  private Integer filasOk = 0;

  @Column(name = "filas_error", nullable = false)
  private Integer filasError = 0;

  @Column(name = "ruta_log", length = 255)
  private String rutaLog;

  @PrePersist
  void prePersist() {
    if (fechaHora == null) fechaHora = LocalDateTime.now();
  }
}

