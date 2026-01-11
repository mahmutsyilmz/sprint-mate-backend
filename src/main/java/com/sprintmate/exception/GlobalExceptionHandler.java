package com.sprintmate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * 
 * Business Intent:
 * Provides consistent error responses across all endpoints.
 * Maps domain exceptions to appropriate HTTP status codes.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException - returns 404 Not Found.
     * Used when a requested entity doesn't exist in the database.
     *
     * @param ex The exception that was thrown
     * @return ResponseEntity with error details and 404 status
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        var error = new ApiError(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles InvalidRoleException - returns 400 Bad Request.
     * Used when an invalid role name is provided.
     *
     * @param ex The exception that was thrown
     * @return ResponseEntity with error details and 400 status
     */
    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ApiError> handleInvalidRole(InvalidRoleException ex) {
        var error = new ApiError(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles NoPartnerAvailableException - returns 404 Not Found.
     * Used when the matching algorithm can't find a suitable partner.
     *
     * @param ex The exception that was thrown
     * @return ResponseEntity with error details and 404 status
     */
    @ExceptionHandler(NoPartnerAvailableException.class)
    public ResponseEntity<ApiError> handleNoPartnerAvailable(NoPartnerAvailableException ex) {
        var error = new ApiError(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles ActiveMatchExistsException - returns 409 Conflict.
     * Used when a user attempts to match while already in an active match.
     *
     * @param ex The exception that was thrown
     * @return ResponseEntity with error details and 409 status
     */
    @ExceptionHandler(ActiveMatchExistsException.class)
    public ResponseEntity<ApiError> handleActiveMatchExists(ActiveMatchExistsException ex) {
        var error = new ApiError(
            LocalDateTime.now(),
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles RoleNotSelectedException - returns 400 Bad Request.
     * Used when a user attempts to match without selecting a role first.
     *
     * @param ex The exception that was thrown
     * @return ResponseEntity with error details and 400 status
     */
    @ExceptionHandler(RoleNotSelectedException.class)
    public ResponseEntity<ApiError> handleRoleNotSelected(RoleNotSelectedException ex) {
        var error = new ApiError(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles validation errors from @Valid annotations - returns 400 Bad Request.
     * Extracts field-level validation errors for detailed feedback.
     *
     * @param ex The validation exception
     * @return ResponseEntity with validation error details and 400 status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("details", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Standard API error response structure.
     * Provides consistent error format across all endpoints.
     */
    public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message
    ) {}
}
