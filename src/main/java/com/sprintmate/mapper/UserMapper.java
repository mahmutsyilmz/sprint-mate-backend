package com.sprintmate.mapper;

import com.sprintmate.dto.UserPreferenceResponse;
import com.sprintmate.dto.UserResponse;
import com.sprintmate.model.ProjectTheme;
import com.sprintmate.model.User;
import com.sprintmate.model.UserPreference;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper for converting between User entity and DTOs.
 *
 * Business Intent:
 * Centralizes entity-to-DTO conversion logic to ensure consistent
 * data transformation across all services that handle User data.
 * Prevents entity details from leaking to API responses.
 */
@Component
public class UserMapper {

    /**
     * Converts User entity to UserResponse DTO.
     * Handles null role and preference gracefully.
     */
    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getGithubUrl(),
            user.getName(),
            user.getSurname(),
            user.getRole() != null ? user.getRole().name() : null,
            user.getBio(),
            user.getSkills() != null ? new HashSet<>(user.getSkills()) : new HashSet<>(),
            toPreferenceResponse(user.getPreference())
        );
    }

    /**
     * Converts UserPreference entity to UserPreferenceResponse DTO.
     * Returns null if preference is null.
     */
    private UserPreferenceResponse toPreferenceResponse(UserPreference preference) {
        if (preference == null) {
            return null;
        }

        Set<String> themeCodes = Set.of();
        Set<String> themeNames = Set.of();

        if (preference.getPreferredThemes() != null && !preference.getPreferredThemes().isEmpty()) {
            themeCodes = preference.getPreferredThemes().stream()
                    .map(ProjectTheme::getCode)
                    .collect(Collectors.toSet());
            themeNames = preference.getPreferredThemes().stream()
                    .map(ProjectTheme::getDisplayName)
                    .collect(Collectors.toSet());
        }

        return new UserPreferenceResponse(
            preference.getDifficultyPreference(),
            themeCodes,
            themeNames,
            preference.getLearningGoals()
        );
    }
}
