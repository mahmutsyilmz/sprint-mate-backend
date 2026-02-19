package com.sprintmate.repository;

import com.sprintmate.model.Match;
import com.sprintmate.model.SprintReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SprintReview entity operations.
 */
@Repository
public interface SprintReviewRepository extends JpaRepository<SprintReview, UUID> {

    /**
     * Finds a sprint review by its associated match.
     *
     * @param match The match to find the review for
     * @return Optional containing the review if found
     */
    Optional<SprintReview> findByMatch(Match match);

    /**
     * Checks if a review already exists for a match.
     *
     * @param match The match to check
     * @return true if a review exists
     */
    boolean existsByMatch(Match match);
}
