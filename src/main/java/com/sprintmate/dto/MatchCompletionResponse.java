package com.sprintmate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for match completion.
 * 
 * Business Intent:
 * Provides confirmation that a match has been successfully completed,
 * including the completion timestamp. Users are now free to search for new matches.
 *
 * @param matchId     The ID of the completed match
 * @param status      The new status of the match (COMPLETED)
 * @param completedAt Timestamp when the match was marked as completed
 */
@Schema(description = "Response after successfully completing a match")
public record MatchCompletionResponse(
    @Schema(description = "The ID of the completed match")
    UUID matchId,
    
    @Schema(description = "The new status of the match", example = "COMPLETED")
    String status,
    
    @Schema(description = "Timestamp when the match was completed")
    LocalDateTime completedAt
) {
    /**
     * Creates a completion response for a successfully completed match.
     */
    public static MatchCompletionResponse of(UUID matchId, LocalDateTime completedAt) {
        return new MatchCompletionResponse(matchId, "COMPLETED", completedAt);
    }
}
