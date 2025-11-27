package com.colors.savd.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.colors.savd.model.Usuario;
import com.colors.savd.model.seguridad.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>{
    Optional<PasswordResetToken> findByTokenAndUsedAtIsNull(String token);

    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :now or t.usedAt is not null")
    int purgeExpired(@Param("now") LocalDateTime now);

    // Borra tokens activos (no usados) y aún vigentes del usuario
    @Modifying
    @Query("delete from PasswordResetToken t where t.usuario = :usuario and t.usedAt is null and t.expiresAt > :now")
    int deleteActiveByUsuario(@Param("usuario") Usuario usuario, @Param("now") LocalDateTime now);

}
