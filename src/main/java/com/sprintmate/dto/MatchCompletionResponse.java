package com.sprintmate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for match completion.
 *
 * Business Intent:
 * Provides confirmation that a match has been successfully completed,
 * including the completion timestamp, repository URL, and AI-generated review.
 * Users are now free to search for new matches.
 *
 * @param matchId              The ID of the completed match
 * @param status               The new status of the match (COMPLETED)
 * @param completedAt          Timestamp when the match was marked as completed
 * @param repoUrl              The GitHub repository URL for the completed project
 * @param reviewScore          AI-generated score (0-100) based on crisis scenario adherence
 * @param reviewFeedback       Constructive AI-generated feedback
 * @param reviewStrengths      List of identified strengths in the submission
 * @param reviewMissingElements List of missing or incomplete elements
 */
@Schema(description = "Response after successfully completing a match with AI review")
public record MatchCompletionResponse(
    @Schema(description = "The ID of the completed match")
    UUID matchId,

    @Schema(description = "The new status of the match", example = "COMPLETED")
    String status,

    @Schema(description = "Timestamp when the match was completed")
    LocalDateTime completedAt,

    @Schema(description = "The GitHub repository URL for the completed project",
            example = "https://github.com/team/sprint-project",
            nullable = true)
    String repoUrl,

    @Schema(description = "AI-generated review score (0-100)",
            example = "85",
            nullable = true)
    Integer reviewScore,

    @Schema(description = "Constructive AI-generated feedback about the submission",
            nullable = true)
    String reviewFeedback,

    @Schema(description = "List of identified strengths in the submission",
            nullable = true)
    List<String> reviewStrengths,

    @Schema(description = "List of missing or incomplete elements",
            nullable = true)
    List<String> reviewMissingElements
) {
    /**
     * Creates a completion response without review (legacy support).
     */
    public static MatchCompletionResponse of(UUID matchId, LocalDateTime completedAt, String repoUrl) {
        return new MatchCompletionResponse(matchId, "COMPLETED", completedAt, repoUrl, null, null, null, null);
    }

    /**
     * Creates a completion response with AI review data.
     */
    public static MatchCompletionResponse withReview(
            UUID matchId,
            LocalDateTime completedAt,
            String repoUrl,
            Integer reviewScore,
            String reviewFeedback,
            List<String> reviewStrengths,
            List<String> reviewMissingElements) {
        return new MatchCompletionResponse(
                matchId,
                "COMPLETED",
                completedAt,
                repoUrl,
                reviewScore,
                reviewFeedback,
                reviewStrengths,
                reviewMissingElements
        );
    }
}
