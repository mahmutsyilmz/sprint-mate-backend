package com.sprintmate.repository;

import com.sprintmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity persistence operations.
 * Provides standard CRUD operations via JpaRepository and custom queries
 * for user lookup by GitHub identity and matching availability.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their GitHub profile URL.
     * Used during OAuth2 login to determine if the user already exists in our system
     * (existing user update) or needs to be created (new user registration).
     *
     * @param githubUrl The GitHub profile URL (e.g., "https://github.com/username")
     * @return Optional containing the User if found, empty otherwise
     */
    Optional<User> findByGithubUrl(String githubUrl);

    /**
     * Finds the oldest waiting user with the target role who is not in an active match.
     * Implements FIFO queue - first user to start waiting gets matched first.
     * 
     * Logic:
     * - User must have the specified role (e.g., BACKEND if current user is FRONTEND)
     * - User must be actively waiting (waiting_since IS NOT NULL)
     * - User must NOT be participating in any ACTIVE match
     * - Excludes the current user from results
     * - Orders by waiting_since ASC (oldest first = FIFO queue)
     * - Returns only one user (LIMIT 1)
     *
     * @param targetRole The role to search for (opposite of current user's role)
     * @param excludeUserId The current user's ID to exclude from search
     * @return Optional containing the oldest waiting User if found, empty otherwise
     */
    @Query(value = "SELECT * FROM users u " +
           "WHERE u.role = :targetRole " +
           "AND u.id <> :excludeUserId " +
           "AND u.waiting_since IS NOT NULL " +
           "AND u.id NOT IN (" +
           "    SELECT mp.user_id FROM match_participants mp " +
           "    JOIN matches m ON m.id = mp.match_id " +
           "    WHERE m.status = 'ACTIVE'" +
           ") " +
           "ORDER BY u.waiting_since ASC " +
           "LIMIT 1", 
           nativeQuery = true)
    Optional<User> findOldestWaitingByRole(@Param("targetRole") String targetRole, 
                                           @Param("excludeUserId") UUID excludeUserId);

    /**
     * Checks if a user is currently waiting in the queue.
     *
     * @param userId The user's ID
     * @return true if user has a waiting_since timestamp set
     */
    boolean existsByIdAndWaitingSinceIsNotNull(UUID userId);
}
