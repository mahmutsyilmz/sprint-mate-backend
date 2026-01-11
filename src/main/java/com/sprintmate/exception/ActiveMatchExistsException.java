package com.sprintmate.exception;

import java.util.UUID;

/**
 * Exception thrown when a user attempts to find a new match while already in an active one.
 * 
 * Business Intent:
 * Enforces the business rule that users can only have one active match at a time.
 * Prevents double-booking of developers in multiple projects simultaneously.
 * Should result in HTTP 409 Conflict response.
 */
public class ActiveMatchExistsException extends RuntimeException {

    /**
     * Creates a new ActiveMatchExistsException with a descriptive message.
     *
     * @param message Description of the conflict
     */
    public ActiveMatchExistsException(String message) {
        super(message);
    }

    /**
     * Creates a new ActiveMatchExistsException for a specific user.
     *
     * @param userId The user who already has an active match
     */
    public static ActiveMatchExistsException forUser(UUID userId) {
        return new ActiveMatchExistsException(
            String.format("User %s already has an active match", userId)
        );
    }
}
