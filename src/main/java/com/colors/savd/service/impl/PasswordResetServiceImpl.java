package com.colors.savd.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colors.savd.exception.BusinessException;
import com.colors.savd.model.seguridad.PasswordResetToken;
import com.colors.savd.repository.PasswordResetTokenRepository;
import com.colors.savd.repository.UsuarioRepository;
import com.colors.savd.service.PasswordResetService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService{

    private final UsuarioRepository usuarioRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder; // asegúrate de tener un @Bean
    private final ApplicationEventPublisher publisher; // opcional para log/audit

    private static final Duration EXPIRATION = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 5;

    @Override
    public String requestReset(String email) {
        // Respuesta genérica para no filtrar usuarios
        var maybeUser = usuarioRepo.findByEmail(email.trim());
        if (maybeUser.isEmpty()) {
            // Igual simulamos éxito
            return "OK";
        }
        var user = maybeUser.get();

        // invalidar tokens previos del usuario (opcional)
        tokenRepo.purgeExpired(LocalDateTime.now());
        tokenRepo.deleteActiveByUsuario(user, LocalDateTime.now());
        
        String token = UUID.randomUUID().toString();
        var prt = PasswordResetToken.builder()
            .token(token)
            .usuario(user)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plus(EXPIRATION))
            .attempts(0)
            .build();
        tokenRepo.save(prt);

        // DEMO: devolvemos el token (en producción lo envías por correo)
        // publisher.publishEvent(new PasswordResetRequestedEvent(user.getId(), user.getEmail(), token));
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyToken(String token) {
        var prt = tokenRepo.findByTokenAndUsedAtIsNull(token).orElse(null);
        if (prt == null) return false;
        if (LocalDateTime.now().isAfter(prt.getExpiresAt())) return false;
        return true;
    }

    @Override
    public void confirmReset(String token, String newPassword) {
        var prt = tokenRepo.findByTokenAndUsedAtIsNull(token).orElseThrow(() -> new BusinessException("Token inválido o ya utilizado"));
        
        if (LocalDateTime.now().isAfter(prt.getExpiresAt())) {
            throw new BusinessException("El token ha expirado");
        }
        if (prt.getAttempts() != null && prt.getAttempts() >= MAX_ATTEMPTS) {
            throw new BusinessException("Se excedieron los intentos permitidos");
        }

        var user = prt.getUsuario();

        // Cambiar contraseña
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Resetear bloqueos/contadores de login si los tienes
        user.setFailedLogins(0);
        user.setLockedUntil(null);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        usuarioRepo.save(user);

        // Marcar token como usado
        prt.setUsedAt(LocalDateTime.now());
        prt.setAttempts(prt.getAttempts() + 1);
        tokenRepo.save(prt);

        // publisher.publishEvent(new PasswordResetCompletedEvent(user.getId()));
    }  
}
