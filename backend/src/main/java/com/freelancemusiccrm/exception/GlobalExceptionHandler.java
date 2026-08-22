package com.freelancemusiccrm.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler that returns a consistent error response format.
 *
 * Error response format:
 * {
 *   "timestamp": "2024-01-01T12:00:00Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "依頼件名は必須です",
 *   "path": "/api/quote-requests",
 *   "fieldErrors": [{ "field": "subject", "message": "依頼件名は必須です" }]
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ----- Validation errors (400) -----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<Map<String, String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "無効な値です"
                ))
                .collect(Collectors.toList());

        String firstMessage = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("バリデーションエラーが発生しました");

        return buildResponse(HttpStatus.BAD_REQUEST, firstMessage, request.getRequestURI(), fieldErrors);
    }

    // ----- Resource not found (404) -----

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    // ----- Conflict / duplicate (409) -----

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflictException(
            ConflictException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    // ----- Business logic error (422) -----

    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<Map<String, Object>> handleUnprocessableEntityException(
            UnprocessableEntityException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(), List.of());
    }

    // ----- Authentication error (401) -----

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailedException(
            AuthenticationFailedException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), List.of());
    }

    // ----- Account locked (423) -----

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLockedException(
            AccountLockedException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.LOCKED, ex.getMessage(), request.getRequestURI(), List.of());
    }

    // ----- Catch-all (500) -----

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        logger.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);

        // Do not expose internal details to the client
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
            "内部サーバーエラーが発生しました",
                request.getRequestURI(),
                List.of()
        );
    }

    // ----- Helper -----

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message,
            String path,
            List<Map<String, String>> fieldErrors) {

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(status).body(body);
    }
}
