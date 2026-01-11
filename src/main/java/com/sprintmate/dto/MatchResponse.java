package com.sprintmate.dto;

import java.util.UUID;

/**
 * Response DTO for match data returned to clients.
 * 
 * Business Intent:
 * Provides all relevant match information after a successful pairing.
 * Contains details about the match, partner, and assigned project.
 *
 * @param matchId      Unique identifier of the match
 * @param status       Current status of the match (e.g., "ACTIVE")
 * @param meetingUrl   Google Meet link for communication
 * @param partnerName  Name of the matched partner
 * @param partnerRole  Role of the matched partner (FRONTEND/BACKEND)
 * @param projectTitle Title of the assigned project
 * @param projectDescription Description of the assigned project
 */
public record MatchResponse(
    UUID matchId,
    String status,
    String meetingUrl,
    String partnerName,
    String partnerRole,
    String projectTitle,
    String projectDescription
) {}
