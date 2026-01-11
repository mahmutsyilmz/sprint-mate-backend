package com.sprintmate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for completing a match.
 * 
 * Business Intent:
 * Allows participants to mark a match as completed and optionally
 * provide the GitHub repository URL of the completed project.
 *
 * @param githubRepoUrl Optional URL to the GitHub repository containing the project deliverable
 */
@Schema(description = "Request body for completing a match")
public record MatchCompletionRequest(
    @Schema(
        description = "Optional GitHub repository URL for the completed project",
        example = "https://github.com/team/sprint-project",
        nullable = true
    )
    String githubRepoUrl
) {
    /**
     * Creates an empty completion request (no repo URL).
     */
    public static MatchCompletionRequest empty() {
        return new MatchCompletionRequest(null);
    }
}
