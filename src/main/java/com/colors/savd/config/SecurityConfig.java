package com.colors.savd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity //Habilitamos @PreAuthorize
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                //publico
                .requestMatchers("/api/auth/**", "/api/health").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                //ventas
                .requestMatchers(HttpMethod.POST, "/api/ventas/*/anular").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/ventas/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR")
                //importaciones
                .requestMatchers(HttpMethod.POST, "/api/importaciones/ventas").hasAnyAuthority("ROLE_ADMIN","ROLE_ANALISTA")
                //bitacoras 
                .requestMatchers(HttpMethod.GET,  "/api/bitacoras/*/log").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/bitacoras/**").hasAnyAuthority("ROLE_ADMIN","ROLE_ANALISTA")
                //reportes: autenticados
                .requestMatchers("/api/reportes/**").authenticated()
                //todo lo demas: autenticado
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
