package com.example.pandasoft.assignment.api;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> handle(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatus()).body(errorBody(
                exception.getStatus().value(), exception.getCode(), exception.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = errorBody(400, "VALIDATION_ERROR", "Request validation failed.", request);
        body.put("fieldErrors", fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<?> malformed(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(errorBody(
                400, "VALIDATION_ERROR", "Request contains an invalid or missing value.", request));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    ResponseEntity<?> concurrent(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(409).body(errorBody(
                409,
                "INVALID_STATUS_TRANSITION",
                "This request was changed by another action. Reload it and try again.",
                request));
    }

    private Map<String, Object> errorBody(
            int status, String code, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status);
        body.put("error", code);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return body;
    }
}
