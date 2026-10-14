package com.example.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException ex) {

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        return ResponseEntity.status(status).body(Map.of(
                "status", String.valueOf(status.value()),
                "error", status.getReasonPhrase(),
                "message", ex.getReason() != null
                        ? ex.getReason()
                        : "İstek işlenemedi."
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequest(
            HttpMessageNotReadableException ex) {

        return ResponseEntity.badRequest().body(Map.of(
                "status", "400",
                "error", "Bad Request",
                "message", "İstek gövdesi geçersiz veya hatalı."
        ));
    }
}