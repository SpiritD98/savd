package com.colors.savd.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.colors.savd.dto.auth.PasswordResetConfirmDTO;
import com.colors.savd.dto.auth.PasswordResetRequestDTO;
import com.colors.savd.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /** Pide token (respuesta siempre 200 para no filtrar si el correo existe). */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String,String>> forgot(@Valid @RequestBody PasswordResetRequestDTO req) {
        authService.issueResetToken(req.getEmail());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /** (Opcional) Verifica si el token sigue siendo válido (sin consumirlo). */
    @GetMapping("/verify-reset")
    public ResponseEntity<Map<String,Object>> verify(@RequestParam String token) {
        boolean ok = authService.verifyResetToken(token);
        return ResponseEntity.ok(Map.of("valid", ok));
    }

    /** Resetea contraseña (consume token). */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String,String>> reset(@Valid @RequestBody PasswordResetConfirmDTO req) {
        authService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("status","ok"));
    }
}
