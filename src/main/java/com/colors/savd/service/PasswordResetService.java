package com.colors.savd.service;

public interface PasswordResetService {
    /** Genera token y (demo) lo devuelve; en prod se envía por email. */
    String requestReset(String email);
    /** Verifica token sin consumirlo (para flujos de UI). */
    boolean verifyToken(String token);
    /** Consume token y cambia contraseña. */
    void confirmReset(String token, String newPassword);
}
