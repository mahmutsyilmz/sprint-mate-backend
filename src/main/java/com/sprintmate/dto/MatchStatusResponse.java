package com.sprintmate.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for match status - covers both matched and waiting states.
 * 
 * Business Intent:
 * Provides unified response for match finding operation.
 * Can represent either a successful match or a waiting-in-queue status.
 *
 * @param status          Current status: "MATCHED" or "WAITING"
 * @param matchId         Match ID (only if status is MATCHED)
 * @param meetingUrl      Google Meet link (only if status is MATCHED)
 * @param partnerName     Partner's name (only if status is MATCHED)
 * @param partnerRole     Partner's role (only if status is MATCHED)
 * @param projectTitle    Project title (only if status is MATCHED)
 * @param projectDescription Project description (only if status is MATCHED)
 * @param waitingSince    Timestamp when user joined queue (only if status is WAITING)
 * @param queuePosition   Position in queue (only if status is WAITING)
 */
public record MatchStatusResponse(
    String status,
    UUID matchId,
    String meetingUrl,
    String partnerName,
    String partnerRole,
    String projectTitle,
    String projectDescription,
    LocalDateTime waitingSince,
    Integer queuePosition
) {
    
    /**
     * Creates a MATCHED response with full match details.
     */
    public static MatchStatusResponse matched(UUID matchId, String meetingUrl, String partnerName,
                                              String partnerRole, String projectTitle, String projectDescription) {
        return new MatchStatusResponse(
            "MATCHED",
            matchId,
            meetingUrl,
            partnerName,
            partnerRole,
            projectTitle,
            projectDescription,
            null,
            null
        );
    }

    /**
     * Creates a WAITING response for users in queue.
     */
    public static MatchStatusResponse waiting(LocalDateTime waitingSince, Integer queuePosition) {
        return new MatchStatusResponse(
            "WAITING",
            null,
            null,
            null,
            null,
            null,
            null,
            waitingSince,
            queuePosition
        );
    }
}
