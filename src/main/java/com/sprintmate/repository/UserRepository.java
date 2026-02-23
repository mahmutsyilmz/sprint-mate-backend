package com.sprintmate.repository;

import com.sprintmate.model.RoleName;
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
     * Finds the oldest waiting user compatible with the current user's match request.
     *
     * Compatibility rules:
     * - If targetRole is specified: partner must have that role
     * - If targetRole is null (ANY): any role is accepted
     * - Partner's waitingForRole must be null (ANY) OR equal to the current user's role
     * - Partner must be actively waiting and not already in an ACTIVE match
     *
     * @param targetRole    The role desired for the partner (null = ANY)
     * @param currentUserRole The current user's own role (used to check partner compatibility)
     * @param excludeUserId The current user's ID to exclude from search
     * @return Optional containing the oldest compatible waiting User if found, empty otherwise
     */
    @Query(value = "SELECT TOP 1 * FROM users u " +
           "WHERE (:targetRole IS NULL OR u.role = :targetRole) " +
           "AND u.id <> CAST(:excludeUserId AS uniqueidentifier) " +
           "AND u.waiting_since IS NOT NULL " +
           "AND (u.waiting_for_role IS NULL OR u.waiting_for_role = :currentUserRole) " +
           "AND u.id NOT IN (" +
           "    SELECT mp.user_id FROM match_participants mp " +
           "    JOIN matches m ON m.id = mp.match_id " +
           "    WHERE m.status = 'ACTIVE'" +
           ") " +
           "ORDER BY u.waiting_since ASC",
           nativeQuery = true)
    Optional<User> findOldestWaitingCompatible(
            @Param("targetRole") String targetRole,
            @Param("currentUserRole") String currentUserRole,
            @Param("excludeUserId") UUID excludeUserId);

    /**
     * Checks if a user is currently waiting in the queue.
     *
     * @param userId The user's ID
     * @return true if user has a waiting_since timestamp set
     */
    boolean existsByIdAndWaitingSinceIsNotNull(UUID userId);

    /**
     * Counts users with the same role who joined the queue before the given user.
     * Used for calculating queue position.
     *
     * @param role The user's role
     * @param waitingSince The user's waiting timestamp
     * @return Count of users ahead in queue
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.waitingSince < :waitingSince AND u.waitingSince IS NOT NULL")
    int countByRoleAndWaitingSinceBefore(@Param("role") RoleName role, @Param("waitingSince") java.time.LocalDateTime waitingSince);
}
