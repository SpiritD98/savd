package com.colors.savd.config;

import org.springframework.stereotype.Component;

import com.colors.savd.exception.BusinessException;

@Component
public class PasswordPolicy {

    /**
     * Reglas mínimas:
     * - Longitud >= 8
     * - Al menos 1 mayúscula, 1 minúscula, 1 dígito, 1 símbolo
     */
    public void validate(String raw) {
        if (raw == null || raw.length() < 8) {
            throw new BusinessException("La contraseña debe tener al menos 8 caracteres.");
        }
        boolean hasUpper = raw.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = raw.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = raw.chars().anyMatch(Character::isDigit);
        boolean hasSymbol = raw.chars().anyMatch(c -> !Character.isLetterOrDigit(c));

        if (!(hasUpper && hasLower && hasDigit && hasSymbol)) {
            throw new BusinessException("La contraseña debe incluir mayúscula, minúscula, dígito y símbolo.");
        }
    }
}
