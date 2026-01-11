package com.sprintmate.repository;

import com.sprintmate.model.Match;
import com.sprintmate.model.MatchProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MatchProject entity persistence operations.
 * Provides methods for managing projects assigned to matches.
 */
@Repository
public interface MatchProjectRepository extends JpaRepository<MatchProject, UUID> {

    /**
     * Finds the project associated with a match.
     *
     * @param match The match to find the project for
     * @return Optional containing the MatchProject if found
     */
    Optional<MatchProject> findByMatch(Match match);
}
