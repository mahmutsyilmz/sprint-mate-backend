package com.sprintmate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for sending a chat message.
 * Used in WebSocket message handling.
 *
 * @param matchId The ID of the match conversation
 * @param content The message content (max 2000 characters)
 */
public record ChatMessageRequest(
    @NotNull(message = "Match ID is required")
    UUID matchId,

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    String content
) {}
