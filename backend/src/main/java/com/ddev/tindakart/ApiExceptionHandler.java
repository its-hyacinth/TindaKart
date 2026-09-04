package com.ddev.tindakart;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException ex) {
        return body(ex.getStatusCode().value(), ex.getReason() == null ? "Request failed" : ex.getReason());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<?> validation(Exception ex) {
        return body(HttpStatus.BAD_REQUEST.value(), "Request validation failed");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception ex) {
        log.error("Unhandled API error", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected server error");
    }

    private ResponseEntity<?> body(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now(), "status", status, "error", message));
    }
}
