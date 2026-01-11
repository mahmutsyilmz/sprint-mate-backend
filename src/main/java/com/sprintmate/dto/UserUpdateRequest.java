package com.sprintmate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating user profile.
 * 
 * Business Intent:
 * Allows users to update their editable profile fields (name, bio, role).
 * Validates input to ensure data integrity.
 *
 * @param name User's display name (required, max 100 characters)
 * @param bio  User's bio/title (optional, max 255 characters)
 * @param role User's role (optional, must be "FRONTEND" or "BACKEND" if provided)
 */
public record UserUpdateRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name,

    @Size(max = 255, message = "Bio must be at most 255 characters")
    String bio,

    @Pattern(regexp = "^(FRONTEND|BACKEND)$", message = "Role must be either FRONTEND or BACKEND")
    String role
) {}
