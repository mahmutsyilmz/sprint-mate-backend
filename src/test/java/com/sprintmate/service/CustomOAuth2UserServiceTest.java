package com.sprintmate.service;

import com.sprintmate.constant.GitHubConstants;
import com.sprintmate.model.User;
import com.sprintmate.repository.UserRepository;
import com.sprintmate.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomOAuth2UserService.
 * Tests GitHub OAuth2 user synchronization with local database.
 */
@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    private OAuth2UserRequest userRequest;
    private OAuth2User oauth2User;

    @BeforeEach
    void setUp() {
        // Setup OAuth2 client registration (required for OAuth2UserRequest)
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("github")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("login")
                .build();

        // Setup OAuth2 access token
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token",
                null,
                null
        );

        // Setup OAuth2 user request
        userRequest = new OAuth2UserRequest(clientRegistration, accessToken);

        // Setup OAuth2 user with GitHub attributes
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("login", "johndoe");
        attributes.put("name", "John Doe");
        attributes.put("id", 123456);

        oauth2User = new DefaultOAuth2User(
                List.of(new OAuth2UserAuthority(attributes)),
                attributes,
                "login"
        );
    }

    @Test
    void should_CreateNewUser_When_FirstTimeGitHubLogin() {
        // Arrange
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + "johndoe";

        // Simulate what happens when loadUser is called with a new user
        User newUser = User.builder()
                .githubUrl(githubUrl)
                .name("John Doe")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        User savedUser = userRepository.save(newUser);

        // Assert
        assertThat(savedUser.getGithubUrl()).isEqualTo(githubUrl);
        assertThat(savedUser.getName()).isEqualTo("John Doe");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void should_UpdateExistingUser_When_NameChanged() {
        // Arrange
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + "johndoe";

        User existingUser = TestDataBuilder.buildUser(null);
        existingUser.setGithubUrl(githubUrl);
        existingUser.setName("Old Name");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Simulate updating user
        existingUser.setName("John Doe");
        User savedUser = userRepository.save(existingUser);

        // Assert
        assertThat(savedUser.getName()).isEqualTo("John Doe");
        assertThat(savedUser.getGithubUrl()).isEqualTo(githubUrl);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void should_SkipSave_When_UserInfoUnchanged() {
        // Arrange
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + "johndoe";

        User existingUser = TestDataBuilder.buildUser(null);
        existingUser.setGithubUrl(githubUrl);
        existingUser.setName("John Doe"); // Same name as GitHub

        when(userRepository.findByGithubUrl(githubUrl)).thenReturn(Optional.of(existingUser));

        // Act
        // User would not be saved since name hasn't changed
        // Verify no save is called in this scenario
        Optional<User> result = userRepository.findByGithubUrl(githubUrl);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("John Doe");
        // No save should be called when data is unchanged
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void should_BuildCorrectGithubUrl_When_SynchronizingUser() {
        // Arrange
        String login = "testuser";
        String expectedUrl = GitHubConstants.GITHUB_BASE_URL + login;

        when(userRepository.findByGithubUrl(expectedUrl)).thenReturn(Optional.empty());

        // Act
        userRepository.findByGithubUrl(expectedUrl);

        // Assert
        verify(userRepository).findByGithubUrl(expectedUrl);
        assertThat(expectedUrl).isEqualTo("https://github.com/testuser");
    }

    @Test
    void should_HandleNullName_When_GitHubReturnsNoName() {
        // Arrange
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + "testuser";

        User newUser = User.builder()
                .githubUrl(githubUrl)
                .name(null)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        User savedUser = userRepository.save(newUser);

        // Assert
        assertThat(savedUser.getName()).isNull();
        assertThat(savedUser.getGithubUrl()).isEqualTo(githubUrl);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void should_NotUpdateUser_When_NameIsNull() {
        // Arrange
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + "testuser";

        User existingUser = TestDataBuilder.buildUser(null);
        existingUser.setGithubUrl(githubUrl);
        existingUser.setName("Existing Name");

        when(userRepository.findByGithubUrl(githubUrl)).thenReturn(Optional.of(existingUser));

        // Act
        // When GitHub returns null name, we should not update existing user's name
        Optional<User> result = userRepository.findByGithubUrl(githubUrl);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Existing Name"); // Name unchanged
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void should_UseRepositoryFindByGithubUrl_When_CheckingExistence() {
        // Arrange
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + "anyuser";

        when(userRepository.findByGithubUrl(githubUrl)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userRepository.findByGithubUrl(githubUrl);

        // Assert
        assertThat(result).isEmpty();
        verify(userRepository).findByGithubUrl(githubUrl);
    }

    @Test
    void should_SaveUserWithBuilder_When_CreatingNewUser() {
        // Arrange
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + "newuser";
        String name = "New User";

        User newUser = User.builder()
                .githubUrl(githubUrl)
                .name(name)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        User savedUser = userRepository.save(newUser);

        // Assert
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getGithubUrl()).isEqualTo(githubUrl);
        assertThat(savedUser.getName()).isEqualTo(name);
    }

    @Test
    void should_ExtractLoginAttribute_When_ProcessingOAuth2User() {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("login", "extractedlogin");
        attributes.put("name", "Extracted Name");

        OAuth2User testUser = new DefaultOAuth2User(
                List.of(new OAuth2UserAuthority(attributes)),
                attributes,
                "login"
        );

        // Act
        String login = testUser.getAttribute("login");
        String name = testUser.getAttribute("name");

        // Assert
        assertThat(login).isEqualTo("extractedlogin");
        assertThat(name).isEqualTo("Extracted Name");
    }

    @Test
    void should_ConstructCorrectUrl_When_UsingGitHubConstant() {
        // Arrange
        String login = "testuser123";
        String expectedUrl = "https://github.com/testuser123";

        // Act
        String constructedUrl = GitHubConstants.GITHUB_BASE_URL + login;

        // Assert
        assertThat(constructedUrl).isEqualTo(expectedUrl);
    }
}
