package com.colors.savd.config;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import com.colors.savd.model.Usuario;
import com.colors.savd.repository.UsuarioRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventsListener{
    private static final int MAX_FAILED = 5;         // umbral de intentos
    private static final int LOCK_MINUTES = 15;      // bloqueo temporal

    private final UsuarioRepository usuarioRepository;

    @Component
    @RequiredArgsConstructor
    public static class AuthSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {
        private final UsuarioRepository usuarioRepository;

        @Override @Transactional
        public void onApplicationEvent(AuthenticationSuccessEvent event) {
            String email = event.getAuthentication().getName();
            Optional<Usuario> opt = usuarioRepository.findByEmail(email);
            if (opt.isEmpty()) return;

            Usuario u = opt.get();
            u.setLastLoginAt(LocalDateTime.now());
            u.setFailedLogins(0);
            u.setLockedUntil(null);
            usuarioRepository.save(u);
        }
    }


    @Component
    @RequiredArgsConstructor
    public static class AuthFailureListener implements ApplicationListener<AbstractAuthenticationFailureEvent> {
        private final UsuarioRepository usuarioRepository;

        @Override @Transactional
        public void onApplicationEvent(AbstractAuthenticationFailureEvent event) {
            String email = event.getAuthentication().getName();
            Optional<Usuario> opt = usuarioRepository.findByEmail(email);
            if (opt.isEmpty()) return;

            Usuario u = opt.get();

            // si ya está bloqueado y no ha expirado, no acumules
            if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now())) {
                return;
            }

            int fails = u.getFailedLogins() + 1;
            if (fails >= MAX_FAILED) {
                u.setFailedLogins(0);
                u.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                log.warn("Cuenta bloqueada por intentos fallidos: {}", email);
            } else {
                u.setFailedLogins(fails);
            }
            usuarioRepository.save(u);
        }
    }
}
