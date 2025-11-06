package com.colors.savd.exception;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MissingPathVariableException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;


@RestControllerAdvice @Slf4j
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

    // --- Snippets "drop-in" 401 No autenticado
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req){
        ApiError body = ApiError.builder()
        .timestamp(Instant.now())
        .status(HttpStatus.UNAUTHORIZED.value())
        .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
        .mensaje("No autenticado o credenciales invalidas")
        .path(req.getRequestURI())
        .code("UNAUTHORIZED")
        .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // 403 Prohibido
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req){
        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error(HttpStatus.FORBIDDEN.getReasonPhrase())
            .mensaje("Acceso denegado")
            .path(req.getRequestURI())
            .code("FORBIDDEN")
            .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // 400 Parámetros inválidos / faltantes
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
        MissingPathVariableException.class, BindException.class})
    public ResponseEntity<ApiError> handleBadParams(Exception ex, HttpServletRequest req){
        String msg;
        if (ex instanceof MethodArgumentTypeMismatchException e) {
            msg = "Parámetro inválido: " + e.getName();
        } else if (ex instanceof MissingServletRequestParameterException e) {
            msg = "Falta parámetro: " + e.getParameterName();
        } else if (ex instanceof MissingPathVariableException e) {
            msg = "Falta variable de ruta: " + e.getVariableName();
        } else if (ex instanceof BindException e) {
            msg = e.getAllErrors().isEmpty() ? "Parámetros inválidos"
                : e.getAllErrors().get(0).getDefaultMessage();
        } else {
            msg = "Solicitud inválida";
        }
        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .mensaje(msg)
            .path(req.getRequestURI())
            .code("BAD_REQUEST")
            .build();
        return ResponseEntity.badRequest().body(body);
    }

    // 400 Multipart / subida de archivo
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> handleMultipart(MultipartException ex, HttpServletRequest req){
        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .mensaje("Error con archivo adjunto o formato multipart")
            .path(req.getRequestURI())
            .code("MULTIPART_ERROR")
            .build();
        return ResponseEntity.badRequest().body(body);
    }    

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req){
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.BAD_REQUEST;
        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .mensaje(ex.getReason() != null ? ex.getReason() : "Error")
            .path(req.getRequestURI())
            .code("ERROR")
            .build();
        return ResponseEntity.status(status).body(body);
    }

    //  --- Violaciones de integridad (FK/UK/Checks) ---
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : null;
        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.CONFLICT.value())
            .error(HttpStatus.CONFLICT.getReasonPhrase())
            .mensaje((cause != null && !cause.isBlank())
                ? ("Restricción de datos: " + cause)
                : "Operación no permitida por restricciones de datos")
            .path(req.getRequestURI())
            .code("DATA_CONFLICT")
            .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // 400 IllegalArgumentException 
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req){
        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .mensaje(ex.getMessage() != null ? ex.getMessage() : "Parámetros inválidos")
            .path(req.getRequestURI())
            .code("BAD_REQUEST")
            .build();
        return ResponseEntity.badRequest().body(body);
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

    // --- Fallback: errores no controlados ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
            log.error("Unexpected error at {}: {}", req.getRequestURI(), ex.getMessage(),ex);
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

    //405 / 415 explícitos - Útiles cuando el cliente usa un método HTTP no permitido o un Content-Type no soportado.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req){
        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.METHOD_NOT_ALLOWED.value())
            .error(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
            .mensaje("Método HTTP no permitido")
            .path(req.getRequestURI())
            .code("METHOD_NOT_ALLOWED")
            .build();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req){
        ApiError body = ApiError.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
            .error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
            .mensaje("Tipo de contenido no soportado")
            .path(req.getRequestURI())
            .code("UNSUPPORTED_MEDIA_TYPE")
            .build();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }
}
