package com.sprintmate.exception;

import java.util.UUID;

/**
 * Exception thrown when a user attempts to find a match without having selected a role.
 * 
 * Business Intent:
 * Enforces the business rule that users must select their role (FRONTEND/BACKEND)
 * before they can be matched with a partner.
 * Should result in HTTP 400 Bad Request response.
 */
public class RoleNotSelectedException extends RuntimeException {

    /**
     * Creates a new RoleNotSelectedException with a descriptive message.
     *
     * @param message Description of the error
     */
    public RoleNotSelectedException(String message) {
        super(message);
    }

    /**
     * Creates a new RoleNotSelectedException for a specific user.
     *
     * @param userId The user who hasn't selected a role
     */
    public static RoleNotSelectedException forUser(UUID userId) {
        return new RoleNotSelectedException(
            String.format("User %s must select a role before matching", userId)
        );
    }
}
