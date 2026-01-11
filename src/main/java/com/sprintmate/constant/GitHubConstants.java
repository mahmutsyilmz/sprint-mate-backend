package com.sprintmate.constant;

/**
 * Constants related to GitHub integration.
 * 
 * Business Intent:
 * Centralizes GitHub-related configuration values to ensure consistency
 * across OAuth2 authentication and user profile management.
 */
public final class GitHubConstants {

    /**
     * Base URL for GitHub user profiles.
     * Used to construct full GitHub profile URLs from usernames.
     * Example: https://github.com/username
     */
    public static final String GITHUB_BASE_URL = "https://github.com/";

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static constants.
     */
    private GitHubConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
