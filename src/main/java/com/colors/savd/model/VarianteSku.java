package com.colors.savd.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.colors.savd.model.enums.EstatusRegistro;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "variante_sku", uniqueConstraints = {
    @UniqueConstraint(name = "uq_variante_sku__sku", columnNames = "sku"),
    @UniqueConstraint(name = "uq_variante_sku__prod_talla_color", columnNames = {"producto_id", "talla_id", "color_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VarianteSku {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length = 80)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", foreignKey = @ForeignKey(name = "fk_variante_sku__producto"))
    private Producto producto;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="talla_id", foreignKey=@ForeignKey(name="fk_variante_sku__talla"))
    private Talla talla;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="color_id", foreignKey=@ForeignKey(name="fk_variante_sku__color"))
    private Color color;

    @Column(name="precio_lista", nullable=false, precision=10, scale=2)
    private BigDecimal precioLista = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean activo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus_registro", nullable = false, length = 12)
    private EstatusRegistro estatusRegistro = EstatusRegistro.VISIBLE;

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
