package com.sprintmate.mapper;

import com.sprintmate.dto.ChatMessageResponse;
import com.sprintmate.model.ChatMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting between ChatMessage entity and DTOs.
 * Centralizes entity-to-DTO conversion for chat messages.
 */
@Component
public class ChatMapper {

    /**
     * Converts ChatMessage entity to ChatMessageResponse DTO.
     *
     * @param message The chat message entity to convert
     * @return ChatMessageResponse DTO with message data
     */
    public ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
            message.getId(),
            message.getMatchId(),
            message.getSenderId(),
            message.getSenderName(),
            message.getContent(),
            message.getCreatedAt()
        );
    }

    /**
     * Converts a list of ChatMessage entities to ChatMessageResponse DTOs.
     *
     * @param messages The list of chat message entities
     * @return List of ChatMessageResponse DTOs
     */
    public List<ChatMessageResponse> toResponseList(List<ChatMessage> messages) {
        return messages.stream()
            .map(this::toResponse)
            .toList();
    }
}
