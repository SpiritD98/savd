package com.colors.savd.model;

import java.time.LocalDateTime;

import com.colors.savd.model.enums.EstadoNegocio;
import com.colors.savd.model.enums.EstatusRegistro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    
    private EstadoNegocio estadoNegocio = EstadoNegocio.ACTIVO;
    private EstatusRegistro estatusRegistro = EstatusRegistro.VISIBLE;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
