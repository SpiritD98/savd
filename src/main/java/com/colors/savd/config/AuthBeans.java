package com.colors.savd.config;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.colors.savd.model.enums.EstadoNegocio;
import com.colors.savd.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AuthBeans {
    private final UsuarioRepository usuarioRepository;

    @Bean
    public UserDetailsService userDetailsService(){
        return username -> {
            var usuario = usuarioRepository.findByEmailWithRol(username).orElseThrow(() -> new UsernameNotFoundException("No existe: "+ username));
            if (usuario.getEstadoNegocio() != EstadoNegocio.ACTIVO) {
                throw new UsernameNotFoundException("Usuario inactivo: "+ username);
            }

            boolean locked = usuario.getLockedUntil() != null && usuario.getLockedUntil().isAfter(LocalDateTime.now());
            String roleCode = usuario.getRol().getCodigo();
            var authority = new SimpleGrantedAuthority("ROLE_" + roleCode);

            return User.withUsername(usuario.getEmail())
                        .password(usuario.getPasswordHash())
                        .authorities(authority)
                        .accountLocked(locked)
                        .disabled(false)
                        .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
