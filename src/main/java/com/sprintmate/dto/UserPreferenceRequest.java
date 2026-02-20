package com.sprintmate.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Request DTO for updating user project preferences.
 *
 * @param difficultyPreference Difficulty level (1=beginner, 2=intermediate, 3=advanced)
 * @param preferredThemeCodes  Set of theme codes (e.g., "finance", "health")
 * @param learningGoals        Comma-separated learning goals (e.g., "WebSocket,GraphQL")
 */
public record UserPreferenceRequest(
    @Min(value = 1, message = "Difficulty must be at least 1")
    @Max(value = 3, message = "Difficulty must be at most 3")
    Integer difficultyPreference,

    Set<String> preferredThemeCodes,

    @Size(max = 500, message = "Learning goals must be at most 500 characters")
    String learningGoals
) {}
