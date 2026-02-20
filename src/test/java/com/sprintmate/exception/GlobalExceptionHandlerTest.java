package com.sprintmate.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests exception mapping to HTTP status codes and error response formatting.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void should_Return404_When_ResourceNotFoundException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        ResourceNotFoundException exception = new ResourceNotFoundException("User", "id", userId);

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiError> response =
                exceptionHandler.handleResourceNotFound(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).contains("User");
        assertThat(response.getBody().message()).contains(userId.toString());
        assertThat(response.getBody().timestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    void should_Return400_When_InvalidRoleException() {
        // Arrange
        InvalidRoleException exception = new InvalidRoleException("INVALID_ROLE");

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiError> response =
                exceptionHandler.handleInvalidRole(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).contains("INVALID_ROLE");
    }

    @Test
    void should_Return404_When_NoPartnerAvailableException() {
        // Arrange
        NoPartnerAvailableException exception = new NoPartnerAvailableException("No matching partner found");

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiError> response =
                exceptionHandler.handleNoPartnerAvailable(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("No matching partner found");
    }

    @Test
    void should_Return409_When_ActiveMatchExistsException() {
        // Arrange
        ActiveMatchExistsException exception = new ActiveMatchExistsException("User already has an active match");

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiError> response =
                exceptionHandler.handleActiveMatchExists(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().message()).contains("active match");
    }

    @Test
    void should_Return400_When_RoleNotSelectedException() {
        // Arrange
        RoleNotSelectedException exception = new RoleNotSelectedException("Please select a role before matching");

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiError> response =
                exceptionHandler.handleRoleNotSelected(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("select a role");
    }

    @Test
    void should_Return403_When_AccessDeniedException() {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException("User not a participant in this match");

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiError> response =
                exceptionHandler.handleAccessDenied(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().error()).isEqualTo("Forbidden");
        assertThat(response.getBody().message()).contains("not a participant");
    }

    @Test
    void should_Return400_When_IllegalStateException() {
        // Arrange
        IllegalStateException exception = new IllegalStateException("Match is not in ACTIVE state");

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiError> response =
                exceptionHandler.handleIllegalState(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("not in ACTIVE state");
    }

    @Test
    void should_Return404_When_ReadmeNotFoundException() {
        // Arrange
        ReadmeNotFoundException exception = new ReadmeNotFoundException("owner", "repo");

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiError> response =
                exceptionHandler.handleReadmeNotFound(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("README Not Found");
        assertThat(response.getBody().message()).contains("owner");
        assertThat(response.getBody().message()).contains("repo");
    }

    @Test
    void should_Return400WithFieldErrors_When_ValidationFails() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("userRequest", "name", "Name is required");
        FieldError fieldError2 = new FieldError("userRequest", "role", "Role must be either FRONTEND or BACKEND");

        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // Act
        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.handleValidationErrors(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("error")).isEqualTo("Validation Failed");

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) response.getBody().get("details");
        assertThat(details).hasSize(2);
        assertThat(details.get("name")).isEqualTo("Name is required");
        assertThat(details.get("role")).isEqualTo("Role must be either FRONTEND or BACKEND");
    }

    @Test
    void should_IncludeTimestamp_When_ExceptionHandled() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException("Match", "id", UUID.randomUUID());

        LocalDateTime beforeHandling = LocalDateTime.now();

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiError> response =
                exceptionHandler.handleResourceNotFound(exception);

        LocalDateTime afterHandling = LocalDateTime.now();

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().timestamp()).isAfter(beforeHandling.minusSeconds(1));
        assertThat(response.getBody().timestamp()).isBefore(afterHandling.plusSeconds(1));
    }

    @Test
    void should_FormatErrorCorrectly_When_MultipleExceptions() {
        // Test that different exceptions produce consistent error format

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiError> response1 =
                exceptionHandler.handleInvalidRole(new InvalidRoleException("TEST"));
        ResponseEntity<GlobalExceptionHandler.ApiError> response2 =
                exceptionHandler.handleRoleNotSelected(new RoleNotSelectedException("TEST"));

        // Assert - Both should have same structure
        assertThat(response1.getBody()).isNotNull();
        assertThat(response2.getBody()).isNotNull();

        assertThat(response1.getBody().timestamp()).isNotNull();
        assertThat(response1.getBody().status()).isNotNull();
        assertThat(response1.getBody().error()).isNotNull();
        assertThat(response1.getBody().message()).isNotNull();

        assertThat(response2.getBody().timestamp()).isNotNull();
        assertThat(response2.getBody().status()).isNotNull();
        assertThat(response2.getBody().error()).isNotNull();
        assertThat(response2.getBody().message()).isNotNull();
    }

    @Test
    void should_PreserveOriginalMessage_When_ExceptionHandled() {
        // Arrange
        String originalMessage = "Custom error message for testing";
        IllegalStateException exception = new IllegalStateException(originalMessage);

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiError> response =
                exceptionHandler.handleIllegalState(exception);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo(originalMessage);
    }
}
