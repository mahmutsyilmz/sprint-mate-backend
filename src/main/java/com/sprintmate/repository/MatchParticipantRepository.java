package com.sprintmate.repository;

import com.sprintmate.model.Match;
import com.sprintmate.model.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for MatchParticipant entity persistence operations.
 * Provides methods for managing participants in matches.
 */
@Repository
public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, UUID> {

    /**
     * Finds all participants for a given match.
     *
     * @param match The match to find participants for
     * @return List of match participants
     */
    List<MatchParticipant> findByMatch(Match match);
}
