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

    @Column(name = "reset_token", length = 180)
    private String resetToken;

    @Column(name = "reset_token_expira")
    private LocalDateTime resetTokenExpira;

    @Column(name = "failed_logins", nullable = false)
    private int failedLogins = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
