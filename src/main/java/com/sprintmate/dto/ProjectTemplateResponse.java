package com.sprintmate.dto;

import java.util.UUID;

/**
 * Response DTO for project template data returned to clients.
 * 
 * Business Intent:
 * Provides a clean representation of project templates for API responses.
 * Used when listing available projects or returning assigned project details.
 *
 * @param id          Unique identifier of the project template
 * @param title       Project title (e.g., "E-commerce Dashboard")
 * @param description Detailed description with requirements and specifications
 */
public record ProjectTemplateResponse(
    UUID id,
    String title,
    String description
) {}
