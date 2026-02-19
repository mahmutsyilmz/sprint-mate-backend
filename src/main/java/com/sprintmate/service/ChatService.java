package com.sprintmate.service;

import com.sprintmate.dto.ChatMessageRequest;
import com.sprintmate.dto.ChatMessageResponse;
import com.sprintmate.mapper.ChatMapper;
import com.sprintmate.model.ChatMessage;
import com.sprintmate.model.User;
import com.sprintmate.repository.ChatMessageRepository;
import com.sprintmate.repository.MatchParticipantRepository;
import com.sprintmate.repository.UserRepository;
import com.sprintmate.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing chat messages in match conversations.
 * Handles message persistence, history retrieval, and access validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final UserRepository userRepository;
    private final ChatMapper chatMapper;

    private static final int DEFAULT_HISTORY_LIMIT = 100;

    /**
     * Saves a new chat message to the database.
     * Validates that the sender is a participant in the match.
     *
     * @param request  The chat message request containing matchId and content
     * @param senderId The UUID of the user sending the message
     * @return The saved message as a response DTO
     * @throws AccessDeniedException if sender is not a participant
     */
    @Transactional
    public ChatMessageResponse saveMessage(ChatMessageRequest request, UUID senderId) {
        // Validate sender is a participant in the match
        if (!canAccessChat(request.matchId(), senderId)) {
            log.warn("User {} attempted to send message to match {} but is not a participant",
                     senderId, request.matchId());
            throw new AccessDeniedException("You are not a participant in this match");
        }

        // Get sender's name for display
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        String senderName = buildDisplayName(sender);

        // Create and save the message
        ChatMessage message = ChatMessage.builder()
            .matchId(request.matchId())
            .senderId(senderId)
            .senderName(senderName)
            .content(request.content())
            .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("Saved chat message {} for match {} from user {}",
                 savedMessage.getId(), request.matchId(), senderId);

        return chatMapper.toResponse(savedMessage);
    }

    /**
     * Retrieves chat history for a match conversation.
     * Returns the most recent messages up to the specified limit.
     *
     * @param matchId The match ID to retrieve history for
     * @param userId  The user requesting history (for access validation)
     * @param limit   Maximum number of messages to retrieve
     * @return List of messages ordered by creation time ascending
     * @throws AccessDeniedException if user is not a participant
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(UUID matchId, UUID userId, int limit) {
        // Validate user is a participant
        if (!canAccessChat(matchId, userId)) {
            log.warn("User {} attempted to access chat history for match {} but is not a participant",
                     userId, matchId);
            throw new AccessDeniedException("You are not a participant in this match");
        }

        int effectiveLimit = limit > 0 ? limit : DEFAULT_HISTORY_LIMIT;

        // Get messages in descending order, then reverse for chronological display
        List<ChatMessage> messages = chatMessageRepository
            .findByMatchIdOrderByCreatedAtDesc(matchId, PageRequest.of(0, effectiveLimit));

        // Reverse to get chronological order (oldest first)
        Collections.reverse(messages);

        log.debug("Retrieved {} chat messages for match {}", messages.size(), matchId);
        return chatMapper.toResponseList(messages);
    }

    /**
     * Retrieves chat history with default limit.
     *
     * @param matchId The match ID to retrieve history for
     * @param userId  The user requesting history
     * @return List of messages
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(UUID matchId, UUID userId) {
        return getChatHistory(matchId, userId, DEFAULT_HISTORY_LIMIT);
    }

    /**
     * Checks if a user can access the chat for a given match.
     * Used for validating WebSocket subscriptions and REST access.
     *
     * @param matchId The match ID to check access for
     * @param userId  The user ID to check
     * @return true if the user is a participant in the match
     */
    public boolean canAccessChat(UUID matchId, UUID userId) {
        return matchParticipantRepository.existsByMatchIdAndUserId(matchId, userId);
    }

    /**
     * Builds a display name for a user.
     */
    private String buildDisplayName(User user) {
        if (user.getSurname() != null && !user.getSurname().isEmpty()) {
            return user.getName() + " " + user.getSurname();
        }
        return user.getName();
    }
}
