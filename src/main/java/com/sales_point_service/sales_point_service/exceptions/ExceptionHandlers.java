package com.sales_point_service.sales_point_service.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ExceptionHandlers {
    @ExceptionHandler(SalePointException.class)
    public ResponseEntity<Object> handleSalePointException(SalePointException e, WebRequest request) {
        HttpStatus status = (e.getHttpStatus() != null) ? e.getHttpStatus() : HttpStatus.INTERNAL_SERVER_ERROR;

        log.error("SalePointException capturada: {} - Status: {}", e.getMessage(), status, e);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", e.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(CostException.class)
    public ResponseEntity<Object> handleCostException(CostException e, WebRequest request) {
        HttpStatus status = (e.getHttpStatus() != null) ? e.getHttpStatus() : HttpStatus.INTERNAL_SERVER_ERROR;

        log.error("CostException capturada: {} - Status: {}", e.getMessage(), status, e);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", e.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("AccessDeniedException capturada: {} en la ruta: {}", ex.getMessage(), request.getDescription(false).replace("uri=", ""));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", HttpStatus.FORBIDDEN.getReasonPhrase());
        body.put("message", "Acceso denegado. No tiene los permisos necesarios para realizar esta acción.");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception e, WebRequest request) {
        log.error("Excepción genérica no capturada: {}", e.getMessage(), e);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        body.put("message", "Ocurrió un error interno inesperado en el servidor.");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
