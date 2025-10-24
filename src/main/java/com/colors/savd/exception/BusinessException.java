package com.colors.savd.exception;

/**
 * Excepción de negocio (reglas de dominio/validaciones).
 * Lanza esta excepción cuando una operación no cumple
 * reglas funcionales (no es error técnico).
 */

public class BusinessException extends RuntimeException{
    private final String code;

    public BusinessException(String mensaje){
        super(mensaje);
        this.code = "BUSINESS";
    }

    public BusinessException(String code, String mensaje) {
        super(mensaje);
        this.code = code != null ? code : "BUSINESS";
    }

    public BusinessException(String mensaje, Throwable cause){
        super(mensaje, cause);
        this.code = "BUSINESS";
    }

    public String getCode() {
        return code;
    }
}
