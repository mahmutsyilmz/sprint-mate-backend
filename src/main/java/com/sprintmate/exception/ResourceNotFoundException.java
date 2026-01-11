package com.sprintmate.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 * 
 * Business Intent:
 * Provides a clear indication that the requested entity (User, Match, etc.)
 * does not exist in the database. Should result in HTTP 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates a new ResourceNotFoundException with a descriptive message.
     *
     * @param message Description of what resource was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new ResourceNotFoundException for a specific resource type and identifier.
     *
     * @param resourceName Name of the resource type (e.g., "User", "Match")
     * @param fieldName    Name of the field used for lookup (e.g., "id", "githubUrl")
     * @param fieldValue   Value that was searched for
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
