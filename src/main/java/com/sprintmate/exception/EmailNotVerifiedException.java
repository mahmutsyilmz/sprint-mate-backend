package com.sprintmate.exception;

import java.util.UUID;

/**
 * Exception thrown when a user attempts to join the matching queue
 * without having verified their email address.
 *
 * Maps to HTTP 400 Bad Request via GlobalExceptionHandler.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }

    public static EmailNotVerifiedException forUser(UUID userId) {
        return new EmailNotVerifiedException(
            "User " + userId + " must verify their email before matching"
        );
    }
}
