package com.sprintmate.dto;

/**
 * Response DTO for project themes.
 *
 * @param code        Theme code (e.g., "finance")
 * @param displayName Theme display name (e.g., "Finance")
 */
public record ProjectThemeResponse(
    String code,
    String displayName
) {}
