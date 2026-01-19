package com.sprintmate.dto;

import java.util.Set;
import java.util.UUID;

/**
 * Response DTO representing user status including active match information.
 *
 * Business Intent:
 * Provides complete user state on login/refresh to ensure session persistence.
 * If user has an active match, includes all match details so frontend can
 * immediately redirect to the active sprint view.
 *
 * @param id              Unique identifier of the user
 * @param githubUrl       User's GitHub profile URL
 * @param name            User's display name
 * @param surname         User's surname (may be null)
 * @param role            User's selected role (may be null if not yet selected)
 * @param bio             User's bio/title (may be null)
 * @param skills          User's tech stack / skills
 * @param hasActiveMatch  True if user is currently in an active match
 * @param activeMatch     Active match details (null if no active match)
 */
public record UserStatusResponse(
    UUID id,
    String githubUrl,
    String name,
    String surname,
    String role,
    String bio,
    Set<String> skills,
    boolean hasActiveMatch,
    ActiveMatchInfo activeMatch
) {
    /**
     * Nested record containing active match details.
     */
    public record ActiveMatchInfo(
        UUID matchId,
        String communicationLink,
        String partnerName,
        String partnerRole,
        Set<String> partnerSkills,
        String projectTitle,
        String projectDescription
    ) {}
}
