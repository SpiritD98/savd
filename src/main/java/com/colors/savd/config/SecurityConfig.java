package com.colors.savd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity //Habilitamos @PreAuthorize
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Público
                .requestMatchers("/api/auth/**", "/api/health").permitAll()
                .requestMatchers("/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Ventas
                .requestMatchers(HttpMethod.POST, "/api/ventas/*/anular").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/ventas/**").hasAnyRole("ADMIN", "VENDEDOR")
                // Importaciones
                .requestMatchers(HttpMethod.POST, "/api/importaciones/ventas").hasAnyRole("ADMIN", "ANALISTA")
                // Bitácoras
                .requestMatchers(HttpMethod.GET, "/api/bitacoras/*/log").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/bitacoras/**").hasAnyRole("ADMIN", "ANALISTA")
                // Reportes
                .requestMatchers("/api/reportes/**").authenticated()
                //todo lo demas: autenticado
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
