package com.sprintmate.repository;

import com.sprintmate.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ChatMessage entity persistence operations.
 * Provides methods for retrieving chat history by match.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Finds all messages for a given match, ordered by creation time ascending.
     * Used for loading chat history.
     *
     * @param matchId The match ID to find messages for
     * @return List of chat messages ordered by createdAt ascending
     */
    List<ChatMessage> findByMatchIdOrderByCreatedAtAsc(UUID matchId);

    /**
     * Finds recent messages for a given match with pagination.
     * Orders by creation time descending for "most recent first" retrieval.
     *
     * @param matchId  The match ID to find messages for
     * @param pageable Pagination settings (limit)
     * @return List of chat messages
     */
    List<ChatMessage> findByMatchIdOrderByCreatedAtDesc(UUID matchId, Pageable pageable);

    /**
     * Counts total messages in a match conversation.
     *
     * @param matchId The match ID to count messages for
     * @return Number of messages in the match
     */
    long countByMatchId(UUID matchId);
}
