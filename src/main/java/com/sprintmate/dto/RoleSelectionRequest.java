package com.sprintmate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for role selection endpoint.
 * 
 * Business Intent:
 * Allows users to select their developer role (FRONTEND or BACKEND).
 * This determines what type of partner they will be matched with.
 *
 * @param roleName The role to assign (must be "FRONTEND" or "BACKEND")
 */
public record RoleSelectionRequest(
    @NotBlank(message = "Role name is required")
    @Pattern(regexp = "^(FRONTEND|BACKEND)$", message = "Role must be either FRONTEND or BACKEND")
    String roleName
) {}
