package com.sprintmate.repository;

import com.sprintmate.model.Match;
import com.sprintmate.model.MatchCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MatchCompletion entity persistence operations.
 * Provides methods for managing match completion records.
 */
@Repository
public interface MatchCompletionRepository extends JpaRepository<MatchCompletion, UUID> {

    /**
     * Finds the completion record for a given match.
     *
     * @param match The match to find completion record for
     * @return Optional containing the MatchCompletion if found
     */
    Optional<MatchCompletion> findByMatch(Match match);
}
