package com.sprintmate.exception;

/**
 * Exception thrown when an invalid role name is provided.
 * 
 * Business Intent:
 * Ensures only valid role names (FRONTEND, BACKEND) are accepted.
 * Should result in HTTP 400 Bad Request response.
 */
public class InvalidRoleException extends RuntimeException {

    /**
     * Creates a new InvalidRoleException with the invalid role name.
     *
     * @param roleName The invalid role name that was provided
     */
    public InvalidRoleException(String roleName) {
        super(String.format("Invalid role: '%s'. Valid roles are: FRONTEND, BACKEND", roleName));
    }
}
