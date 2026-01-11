package com.sprintmate.repository;

import com.sprintmate.model.Match;
import com.sprintmate.model.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Match entity persistence operations.
 * Provides methods for managing developer matches and checking match status.
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    /**
     * Checks if a user has an active match (status = ACTIVE).
     * Used to prevent users from being matched multiple times simultaneously.
     *
     * @param userId The UUID of the user to check
     * @param status The match status to check for (typically ACTIVE)
     * @return true if the user has a match with the given status
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
           "FROM Match m " +
           "JOIN MatchParticipant mp ON mp.match = m " +
           "WHERE mp.user.id = :userId AND m.status = :status")
    boolean existsActiveMatchForUser(@Param("userId") UUID userId, @Param("status") MatchStatus status);

    /**
     * Finds the active match for a user.
     * Returns the match if the user is currently participating in an active match.
     *
     * @param userId The UUID of the user
     * @param status The match status to look for
     * @return Optional containing the Match if found
     */
    @Query("SELECT m FROM Match m " +
           "JOIN MatchParticipant mp ON mp.match = m " +
           "WHERE mp.user.id = :userId AND m.status = :status")
    Optional<Match> findMatchByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") MatchStatus status);
}
