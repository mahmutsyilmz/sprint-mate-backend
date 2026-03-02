package com.sprintmate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Request DTO for updating user profile.
 *
 * Business Intent:
 * Allows users to update their editable profile fields (name, bio, role, skills, preferences).
 * If email is provided and differs from the current one, emailVerified is automatically
 * reset to false on the backend, requiring re-verification.
 * Validates input to ensure data integrity.
 *
 * @param name       User's display name (required, max 100 characters)
 * @param bio        User's bio/title (optional, max 255 characters)
 * @param role       User's role (optional, must be "FRONTEND" or "BACKEND" if provided)
 * @param skills     User's tech stack / skills (optional)
 * @param preference User's project generation preferences (optional)
 * @param email      User's email address (optional, triggers re-verification if changed)
 */
public record UserUpdateRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name,

    @Size(max = 255, message = "Bio must be at most 255 characters")
    String bio,

    @Pattern(regexp = "^(FRONTEND|BACKEND)$", message = "Role must be either FRONTEND or BACKEND")
    String role,

    @Size(max = 20, message = "Cannot have more than 20 skills")
    Set<@Size(max = 50, message = "Each skill must be at most 50 characters") String> skills,

    @Valid
    UserPreferenceRequest preference,

    @Email(message = "Must be a valid email address")
    String email
) {}
