package com.sprintmate.exception;

import java.util.UUID;

/**
 * Exception thrown when a user's profile is incomplete (e.g., missing display name).
 * Results in HTTP 400 Bad Request.
 */
public class IncompleteProfileException extends RuntimeException {

    public IncompleteProfileException(String message) {
        super(message);
    }

    public static IncompleteProfileException forUser(UUID userId) {
        return new IncompleteProfileException(
            "User " + userId + " has an incomplete profile. A display name is required to find a match."
        );
    }
}
