package com.colors.savd.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.colors.savd.config.PasswordPolicy;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.model.Usuario;
import com.colors.savd.repository.UsuarioRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    @Transactional
    public void issueResetToken(String email){
        Usuario u = usuarioRepo.findByEmail(email).orElseThrow(() -> new BusinessException("Si el correo existe, se enviará el enlace de recuperación."));
        String token = UUID.randomUUID().toString();
        LocalDateTime expira = LocalDateTime.now().plusMinutes(30);

        u.setResetToken(token);
        u.setResetTokenExpira(expira);
        usuarioRepo.save(u);

        // Por ahora: log. (En producción: email)
        // log.info("RESET LINK: /reset?token={}", token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword){
        // política primero y aplicamos reglas
        passwordPolicy.validate(newPassword);
        
        Usuario u = usuarioRepo.findByResetToken(token).orElseThrow(() -> new BusinessException("Token inválido o expirado."));

        if (u.getResetTokenExpira() == null || u.getResetTokenExpira().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Token inválido o expirado.");
        }

        u.setPasswordHash(passwordEncoder.encode(newPassword));
        u.setPasswordUpdatedAt(LocalDateTime.now());
        // invalidar token y limpiar lock/failed
        u.setResetToken(null);
        u.setResetTokenExpira(null);
        u.setFailedLogins(0);
        u.setLockedUntil(null);

        usuarioRepo.save(u);
    }

}
