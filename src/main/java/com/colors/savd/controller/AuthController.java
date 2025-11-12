package com.colors.savd.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.colors.savd.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String,String>> forgot(@RequestParam String email) {
        // respuesta siempre 200 por seguridad (no revelar si existe o no)
        authService.issueResetToken(email);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String,String>> reset(@RequestParam String token, @RequestParam String newPassword) {
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok(Map.of("status","ok"));
    }
}
