package com.sprintmate.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for chat messages.
 * Used for both WebSocket broadcasts and REST history retrieval.
 *
 * @param id         Unique message identifier
 * @param matchId    The match conversation this message belongs to
 * @param senderId   The user who sent the message
 * @param senderName Display name of the sender
 * @param content    The message content
 * @param createdAt  Timestamp when the message was created
 */
public record ChatMessageResponse(
    UUID id,
    UUID matchId,
    UUID senderId,
    String senderName,
    String content,
    LocalDateTime createdAt
) {}
