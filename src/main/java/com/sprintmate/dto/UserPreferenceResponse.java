package com.sprintmate.dto;

import java.util.Set;

/**
 * Response DTO representing user project preferences.
 *
 * @param difficultyPreference Difficulty level (1-3)
 * @param preferredThemeCodes  Set of preferred theme codes
 * @param preferredThemeNames  Set of preferred theme display names
 * @param learningGoals        Comma-separated learning goals
 */
public record UserPreferenceResponse(
    Integer difficultyPreference,
    Set<String> preferredThemeCodes,
    Set<String> preferredThemeNames,
    String learningGoals
) {}
