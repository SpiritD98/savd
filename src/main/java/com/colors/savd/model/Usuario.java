package com.colors.savd.model;

import java.time.LocalDateTime;

import com.colors.savd.model.enums.EstadoNegocio;
import com.colors.savd.model.enums.EstatusRegistro;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "usuario")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Usuario {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 180, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", foreignKey = @ForeignKey(name = "fk_usuario__rol"))
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_negocio", nullable = false, length = 8)
    private EstadoNegocio estadoNegocio = EstadoNegocio.ACTIVO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus_registro", nullable = false, length = 12)
    private EstatusRegistro estatusRegistro = EstatusRegistro.VISIBLE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
