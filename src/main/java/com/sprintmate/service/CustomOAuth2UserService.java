package com.sprintmate.service;

import com.sprintmate.constant.GitHubConstants;
import com.sprintmate.model.User;
import com.sprintmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom OAuth2 user service that intercepts GitHub OAuth2 login success
 * and synchronizes user information with our local database.
 *
 * Business Intent:
 * - When a user logs in via GitHub, we maintain a local User record
 * - New users are automatically registered on first login (zero-friction onboarding)
 * - Existing users have their display name updated if it changed on GitHub
 * - Email is NOT fetched from GitHub — users provide it voluntarily during onboarding
 *   and verify it via OTP before joining the matching queue.
 *
 * This service bridges GitHub identity with our internal User entity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * Intercepts OAuth2 login and synchronizes user data with local database.
     * Called automatically by Spring Security after successful GitHub authentication.
     *
     * Flow:
     * 1. Load user info from GitHub via parent class
     * 2. Extract login and public name attributes
     * 3. Check if user exists in our DB by GitHub URL
     * 4. Create new user or update existing user's display name
     * 5. Return OAuth2User for Spring Security context
     *
     * @param userRequest OAuth2 request containing access token and client registration
     * @return OAuth2User with GitHub attributes for the authenticated session
     * @throws OAuth2AuthenticationException if authentication fails
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        synchronizeUser(oauth2User);
        return oauth2User;
    }

    /**
     * Synchronizes GitHub user data with our local User entity.
     * Creates new user on first login, updates display name if it changed.
     * Email is intentionally not synced from GitHub — it is user-provided.
     *
     * @param oauth2User The authenticated GitHub user
     */
    private void synchronizeUser(OAuth2User oauth2User) {
        String login = oauth2User.getAttribute("login");
        String name = oauth2User.getAttribute("name");

        // Fallback: if GitHub public name is hidden/empty, use the GitHub login username
        String effectiveName = (name != null && !name.isBlank()) ? name : login;

        // Construct GitHub profile URL as unique identifier
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + login;

        var existingUser = userRepository.findByGithubUrl(githubUrl);

        if (existingUser.isPresent()) {
            updateExistingUser(existingUser.get(), effectiveName);
        } else {
            createNewUser(githubUrl, effectiveName);
        }
    }

    /**
     * Updates an existing user's display name if it has changed on GitHub.
     * Email is not touched here — users manage their own email via the verification flow.
     *
     * @param user The existing user entity
     * @param name The current public name from GitHub
     */
    private void updateExistingUser(User user, String name) {
        if (name != null && !name.equals(user.getName())) {
            user.setName(name);
            userRepository.save(user);
            log.info("Updated display name for user: {}", user.getGithubUrl());
        } else {
            log.debug("No updates needed for user: {}", user.getGithubUrl());
        }
    }

    /**
     * Creates a new user entity from GitHub OAuth2 data.
     * Email is left null — the user will provide and verify it during onboarding.
     *
     * @param githubUrl The constructed GitHub profile URL (unique identifier)
     * @param name      The user's display name from GitHub
     */
    private void createNewUser(String githubUrl, String name) {
        var newUser = User.builder()
                .githubUrl(githubUrl)
                .name(name)
                .build();

        userRepository.save(newUser);
        log.info("Created new user: {}", githubUrl);
    }
}
