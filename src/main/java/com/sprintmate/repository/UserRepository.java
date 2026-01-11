package com.sprintmate.repository;

import com.sprintmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity persistence operations.
 * Provides standard CRUD operations via JpaRepository and custom queries
 * for user lookup by GitHub identity.
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
}
