package com.sprintmate.service;

import com.sprintmate.constant.GitHubConstants;
import com.sprintmate.model.User;
import com.sprintmate.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Arrays;

/**
 * Custom OAuth2 user service that intercepts GitHub OAuth2 login success
 * and synchronizes user information with our local database.
 *
 * Business Intent:
 * - When a user logs in via GitHub, we need to maintain a local User record
 * - New users are automatically registered on first login (zero-friction onboarding)
 * - Existing users have their profile info updated if GitHub data changed
 * - Email is fetched from GitHub (via /user/emails if not public) for match notifications
 *
 * This service bridges GitHub identity with our internal User entity.
 */
@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RestClient gitHubApiClient;

    /**
     * Constructor injection. Builds a dedicated RestClient for the GitHub API.
     *
     * @param userRepository    Injected user repository
     * @param restClientBuilder Injected RestClient.Builder bean from RestClientConfig
     */
    public CustomOAuth2UserService(UserRepository userRepository, RestClient.Builder restClientBuilder) {
        this.userRepository = userRepository;
        this.gitHubApiClient = restClientBuilder
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("User-Agent", "SprintMate/1.0")
                .build();
    }

    /**
     * Intercepts OAuth2 login and synchronizes user data with local database.
     * Called automatically by Spring Security after successful GitHub authentication.
     *
     * Flow:
     * 1. Load user info from GitHub via parent class
     * 2. Extract relevant attributes (login, name, email)
     * 3. If email not in main response, call /user/emails API with access token
     * 4. Check if user exists in our DB by GitHub URL
     * 5. Create new user or update existing user's info
     * 6. Return OAuth2User for Spring Security context
     *
     * @param userRequest OAuth2 request containing access token and client registration
     * @return OAuth2User with GitHub attributes for the authenticated session
     * @throws OAuth2AuthenticationException if authentication fails
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegate to parent to fetch user info from GitHub API
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Synchronize GitHub user with our local database
        synchronizeUser(oauth2User, userRequest.getAccessToken().getTokenValue());

        return oauth2User;
    }

    /**
     * Synchronizes GitHub user data with our local User entity.
     * Creates new user on first login, updates existing user if info changed.
     *
     * @param oauth2User  The authenticated GitHub user
     * @param accessToken The OAuth2 access token for making additional API calls
     */
    private void synchronizeUser(OAuth2User oauth2User, String accessToken) {
        String login = oauth2User.getAttribute("login");
        String name = oauth2User.getAttribute("name");

        // Fallback: if GitHub public name is hidden/empty, use the GitHub login username
        String effectiveName = (name != null && !name.isBlank()) ? name : login;

        // Try to get email from the standard user endpoint first (only present when email is public)
        String email = oauth2User.getAttribute("email");

        // If not public, call the /user/emails endpoint to get the primary verified email
        if (email == null || email.isBlank()) {
            email = fetchPrimaryGitHubEmail(accessToken);
        }

        // Construct GitHub profile URL as unique identifier
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + login;

        var existingUser = userRepository.findByGithubUrl(githubUrl);

        if (existingUser.isPresent()) {
            updateExistingUser(existingUser.get(), effectiveName, email);
        } else {
            createNewUser(githubUrl, effectiveName, email);
        }
    }

    /**
     * Calls GitHub's /user/emails API to retrieve the primary verified email.
     * Used when the user's email is not public on their GitHub profile.
     *
     * @param accessToken The OAuth2 access token (requires user:email scope)
     * @return The primary verified email, or null if unavailable
     */
    private String fetchPrimaryGitHubEmail(String accessToken) {
        // Local record for deserializing the /user/emails response array
        record GitHubEmail(String email, boolean verified, boolean primary) {}

        try {
            GitHubEmail[] emails = gitHubApiClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GitHubEmail[].class);

            if (emails == null) return null;

            return Arrays.stream(emails)
                    .filter(e -> e.primary() && e.verified())
                    .map(GitHubEmail::email)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to fetch GitHub emails API: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Updates an existing user's profile information if it has changed.
     *
     * @param user  The existing user entity
     * @param name  The current name from GitHub
     * @param email The current email from GitHub (may be null)
     */
    private void updateExistingUser(User user, String name, String email) {
        boolean updated = false;

        if (name != null && !name.equals(user.getName())) {
            user.setName(name);
            updated = true;
        }

        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
            log.info("Updated existing user: {}", user.getGithubUrl());
        } else {
            log.debug("No updates needed for user: {}", user.getGithubUrl());
        }
    }

    /**
     * Creates a new user entity from GitHub OAuth2 data.
     * UUID is auto-generated by JPA strategy defined in User entity.
     *
     * @param githubUrl The constructed GitHub profile URL
     * @param name      The user's display name from GitHub
     * @param email     The user's email from GitHub (may be null)
     */
    private void createNewUser(String githubUrl, String name, String email) {
        var newUser = User.builder()
                .githubUrl(githubUrl)
                .name(name)
                .email(email)
                .build();

        userRepository.save(newUser);
        log.info("Created new user: {} (email available: {})", githubUrl, email != null);
    }
}
