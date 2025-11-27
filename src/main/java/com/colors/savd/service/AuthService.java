package com.colors.savd.service;

public interface AuthService {
    /** Emite un token de reseteo (idempotente y respuesta genérica). */
    void issueResetToken(String email);

    /** Verifica un token sin consumirlo (útil para pantallas de “ingresa nueva contraseña”). */
    boolean verifyResetToken(String token);

    /** Consume el token y cambia la contraseña (aplica política). */
    void resetPassword(String token, String newPassword);
}

