package com.sprintmate.dto;

import java.util.UUID;

/**
 * Response DTO representing user data returned to clients.
 * 
 * Business Intent:
 * Provides a safe, immutable view of user data without exposing internal entity details.
 * Used for all user-related API responses.
 *
 * @param id        Unique identifier of the user
 * @param githubUrl User's GitHub profile URL
 * @param name      User's display name
 * @param surname   User's surname (may be null)
 * @param role      User's selected role (may be null if not yet selected)
 */
public record UserResponse(
    UUID id,
    String githubUrl,
    String name,
    String surname,
    String role
) {}
