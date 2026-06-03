package com.inholland.banking_app.controllers;

import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.exceptions.ApprovalFailedException;
import com.inholland.banking_app.exceptions.DuplicateResourceException;
import com.inholland.banking_app.exceptions.ForbiddenException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException exception) {
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage()))
                .collect(Collectors.toList());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "VALIDATION_ERROR");
        body.put("message", "Input validation failed");
        body.put("errors", errors);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(DuplicateResourceException exception) {
        log.warn("Conflict: {}", exception.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(AccountStateException.class)
    public ResponseEntity<Map<String, Object>> handleAccountState(AccountStateException exception) {
        log.warn("Invalid account state: {}", exception.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "ACCOUNT_STATE_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbiddenException(ForbiddenException exception) {
        log.warn("Forbidden: {}", exception.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception) {
        log.warn("Bad request: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(BadCredentialsException exception) {
        log.warn("Unauthorized: {}", exception.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage());
    }

    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<Map<String, Object>> handleForbidden(RuntimeException exception) {
        log.warn("Forbidden: {}", exception.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException exception) {
        log.warn("Access denied: {}", exception.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        log.error("Unexpected server error", exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred"
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ApprovalFailedException.class)
    public ResponseEntity<Map<String, Object>> handleApprovalFailed(ApprovalFailedException exception) {
        log.warn("Approval process failed: {}", exception.getMessage());
        // Using UNPROCESSABLE_ENTITY (422) is common for business logic failures
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "APPROVAL_FAILED", exception.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException exception) {
        log.warn("Endpoint not found: {}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND",
            "The requested endpoint does not exist. Please check the URL and try again.");
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSortProperty(PropertyReferenceException exception) {
        log.warn("Invalid sort property: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_SORT_PROPERTY",
            "Invalid sort property: " + exception.getMessage() + ". Please use a valid field name.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
