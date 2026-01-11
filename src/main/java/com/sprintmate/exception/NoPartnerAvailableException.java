package com.sprintmate.exception;

/**
 * Exception thrown when no matching partner is available.
 * 
 * Business Intent:
 * Indicates that the matching algorithm couldn't find a suitable partner
 * with the opposite role who is not already in an active match.
 * Should result in HTTP 404 or appropriate client-friendly response.
 */
public class NoPartnerAvailableException extends RuntimeException {

    /**
     * Creates a new NoPartnerAvailableException with a descriptive message.
     *
     * @param message Description of why no partner was found
     */
    public NoPartnerAvailableException(String message) {
        super(message);
    }

    /**
     * Creates a new NoPartnerAvailableException for a specific role search.
     *
     * @param targetRole The role that was searched for (e.g., "BACKEND")
     */
    public static NoPartnerAvailableException forRole(String targetRole) {
        return new NoPartnerAvailableException(
            String.format("No available partner with role %s found", targetRole)
        );
    }
}
