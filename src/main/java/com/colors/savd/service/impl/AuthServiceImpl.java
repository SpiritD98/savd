package com.colors.savd.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colors.savd.config.PasswordPolicy;
import com.colors.savd.service.AuthService;
import com.colors.savd.service.PasswordResetService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PasswordResetService passwordResetService;
    private final PasswordPolicy passwordPolicy;

    @Override
    public void issueResetToken(String email) {
        // No reveles existencia del usuario (PasswordResetService ya maneja esa lógica)
        passwordResetService.requestReset(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyResetToken(String token) {
        return passwordResetService.verifyToken(token);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        // Política de contraseñas antes de persistir
        passwordPolicy.validate(newPassword);
        passwordResetService.confirmReset(token, newPassword);
    }
}
