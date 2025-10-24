package com.colors.savd.exception;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- Reglas de negocio ---
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest req){
        ApiError body = ApiError.builder()
        .timestamp(Instant.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .mensaje(ex.getMessage())
        .path(req.getRequestURI())
        .code(ex.getCode()) //puede ser "BUSINESS" u otro
        .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // --- Validacion @Valid en @RequestBody (DTOs) ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getAllErrors().isEmpty()
            ? "Solicitud inválida"
            : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .mensaje(message)
            .path(req.getRequestURI())
            .code("VALIDATION")
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // --- Validacion @Validated en parametros (ConstraintViolation) ---
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req){
        String message = ex.getConstraintViolations().stream()
            .findFirst()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .orElse("Parametros invalidos");

        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .mensaje(message)
            .path(req.getRequestURI())
            .code("VALIDATION")
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // --- JSON malformado, tipos incorrectos en body, etc. ---
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .mensaje("Cuerpo de la solicitud inválido o malformado")
            .path(req.getRequestURI())
            .code("BAD_REQUEST_BODY")
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // --- No encontrado a nivel JPA/servicio ---
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest req) {
        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
            .mensaje(ex.getMessage() != null ? ex.getMessage() : "Recurso no encontrado")
            .path(req.getRequestURI())
            .code("NOT_FOUND")
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // --- Violaciones de integridad (FK/UK/Checks) ---
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.CONFLICT.value())
            .error(HttpStatus.CONFLICT.getReasonPhrase())
            .mensaje("Operación no permitida por restricciones de datos")
            .path(req.getRequestURI())
            .code("DATA_CONFLICT")
            .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // --- Fallback: errores no controlados ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .mensaje("Error inesperado")
            .path(req.getRequestURI())
            .code("UNEXPECTED")
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
