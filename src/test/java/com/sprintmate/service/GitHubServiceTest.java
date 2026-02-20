package com.sprintmate.service;

import com.sprintmate.exception.ReadmeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GitHubService.
 * Tests README fetching logic and error handling.
 *
 * Validates Bug #7 fix: RestClient is now injected via RestClient.Builder
 * instead of being created directly in the constructor.
 */
@ExtendWith(MockitoExtension.class)
class GitHubServiceTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private GitHubService gitHubService;

    @BeforeEach
    void setUp() {
        // Setup fluent RestClient mock chain (lenient to avoid UnnecessaryStubbingException)
        lenient().when(restClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.build()).thenReturn(restClient);
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.header(anyString(), any(String[].class))).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        gitHubService = new GitHubService(restClientBuilder);
    }

    @Test
    void should_FetchFromMainBranch_When_ReadmeExists() {
        // Arrange
        String repoUrl = "https://github.com/user/repository";
        String expectedContent = "# My Project\n\nThis is a README file.";

        when(responseSpec.body(String.class)).thenReturn(expectedContent);

        // Act
        String result = gitHubService.fetchReadme(repoUrl, null);

        // Assert
        assertThat(result).isEqualTo(expectedContent);

        // Verify main branch was tried
        verify(requestHeadersUriSpec).uri("https://raw.githubusercontent.com/user/repository/main/README.md");
    }

    @Test
    void should_FallbackToMaster_When_MainNotFound() {
        // Arrange
        String repoUrl = "https://github.com/owner/repo";
        String expectedContent = "# README from master";

        // First call (main branch) throws 404, second call (master) succeeds
        when(responseSpec.body(String.class))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        null,
                        null,
                        null
                ))
                .thenReturn(expectedContent);

        // Act
        String result = gitHubService.fetchReadme(repoUrl, null);

        // Assert
        assertThat(result).isEqualTo(expectedContent);

        // Verify both branches were tried
        verify(requestHeadersUriSpec).uri("https://raw.githubusercontent.com/owner/repo/main/README.md");
        verify(requestHeadersUriSpec).uri("https://raw.githubusercontent.com/owner/repo/master/README.md");
    }

    @Test
    void should_ThrowReadmeNotFoundException_When_NotFoundOnBothBranches() {
        // Arrange
        String repoUrl = "https://github.com/owner/repo";

        // Both branches throw 404
        when(responseSpec.body(String.class))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        null,
                        null,
                        null
                ));

        // Act & Assert
        assertThatThrownBy(() -> gitHubService.fetchReadme(repoUrl, null))
                .isInstanceOf(ReadmeNotFoundException.class)
                .hasMessageContaining("owner")
                .hasMessageContaining("repo");

        // Verify both branches were attempted
        verify(requestHeadersUriSpec, times(2)).uri(anyString());
    }

    @Test
    void should_ThrowIllegalArgumentException_When_InvalidUrl() {
        // Arrange
        String invalidUrl = "not-a-github-url";

        // Act & Assert
        assertThatThrownBy(() -> gitHubService.fetchReadme(invalidUrl, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid GitHub repository URL");

        // Verify no HTTP calls were made
        verify(restClient, never()).get();
    }

    @Test
    void should_ParseUrlCorrectly_When_UrlHasTrailingSlash() {
        // Arrange
        String repoUrl = "https://github.com/user/project/";
        String expectedContent = "README content";

        when(responseSpec.body(String.class)).thenReturn(expectedContent);

        // Act
        String result = gitHubService.fetchReadme(repoUrl, null);

        // Assert
        assertThat(result).isNotNull();

        // Verify correct URL was constructed (without double slashes)
        verify(requestHeadersUriSpec).uri("https://raw.githubusercontent.com/user/project/main/README.md");
    }

    @Test
    void should_RemoveGitSuffix_When_RepoUrlEndsWithDotGit() {
        // Arrange
        String repoUrl = "https://github.com/user/repository.git";
        String expectedContent = "README";

        when(responseSpec.body(String.class)).thenReturn(expectedContent);

        // Act
        String result = gitHubService.fetchReadme(repoUrl, null);

        // Assert
        assertThat(result).isNotNull();

        // Verify .git suffix was removed
        verify(requestHeadersUriSpec).uri("https://raw.githubusercontent.com/user/repository/main/README.md");
    }

    @Test
    void should_HandleHttpUrl_When_NotHttps() {
        // Arrange
        String repoUrl = "http://github.com/user/repo";
        String expectedContent = "README";

        when(responseSpec.body(String.class)).thenReturn(expectedContent);

        // Act
        String result = gitHubService.fetchReadme(repoUrl, null);

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    void should_ReturnNull_When_RepositoryIsForbidden() {
        // Arrange
        String repoUrl = "https://github.com/private/repo";

        // Both branches return 403 Forbidden
        when(responseSpec.body(String.class))
                .thenThrow(HttpClientErrorException.Forbidden.create(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "Forbidden",
                        null,
                        null,
                        null
                ));

        // Act & Assert
        assertThatThrownBy(() -> gitHubService.fetchReadme(repoUrl, null))
                .isInstanceOf(ReadmeNotFoundException.class);
    }

    @Test
    void should_ParseUrlWithSubpaths_When_UrlHasAdditionalPath() {
        // Arrange
        String repoUrl = "https://github.com/owner/repository/tree/main/subfolder";
        String expectedContent = "README";

        when(responseSpec.body(String.class)).thenReturn(expectedContent);

        // Act
        String result = gitHubService.fetchReadme(repoUrl, null);

        // Assert
        assertThat(result).isNotNull();

        // Verify only owner and repo are extracted
        verify(requestHeadersUriSpec).uri("https://raw.githubusercontent.com/owner/repository/main/README.md");
    }

    @Test
    void should_ExtractOwnerAndRepo_When_UrlIsValid() {
        // Arrange
        String repoUrl = "https://github.com/spring-projects/spring-boot";
        String expectedContent = "Spring Boot README";

        when(responseSpec.body(String.class)).thenReturn(expectedContent);

        // Act
        String result = gitHubService.fetchReadme(repoUrl, null);

        // Assert
        assertThat(result).isEqualTo(expectedContent);

        verify(requestHeadersUriSpec).uri("https://raw.githubusercontent.com/spring-projects/spring-boot/main/README.md");
    }

    @Test
    void should_HandleSpecialCharacters_When_RepoNameContainsDashes() {
        // Arrange
        String repoUrl = "https://github.com/my-org/my-cool-project-name";
        String expectedContent = "README";

        when(responseSpec.body(String.class)).thenReturn(expectedContent);

        // Act
        String result = gitHubService.fetchReadme(repoUrl, null);

        // Assert
        assertThat(result).isNotNull();

        verify(requestHeadersUriSpec).uri("https://raw.githubusercontent.com/my-org/my-cool-project-name/main/README.md");
    }

    @Test
    void should_ThrowException_When_BothBranchesFailWithGenericError() {
        // Arrange
        String repoUrl = "https://github.com/user/repo";

        // Both branches throw generic exception
        when(responseSpec.body(String.class))
                .thenThrow(new RuntimeException("Network error"));

        // Act & Assert
        assertThatThrownBy(() -> gitHubService.fetchReadme(repoUrl, null))
                .isInstanceOf(ReadmeNotFoundException.class);
    }

    @Test
    void should_SetAuthorizationHeader_When_AccessTokenProvided() {
        // Arrange
        String repoUrl = "https://github.com/user/repo";
        String accessToken = "ghp_test_token_123";
        String expectedContent = "# Private Repo README";

        when(responseSpec.body(String.class)).thenReturn(expectedContent);

        // Act
        String result = gitHubService.fetchReadme(repoUrl, accessToken);

        // Assert
        assertThat(result).isEqualTo(expectedContent);
        verify(requestHeadersUriSpec).header("Authorization", "token " + accessToken);
    }

    @Test
    void should_NotSetAuthorizationHeader_When_AccessTokenIsNull() {
        // Arrange
        String repoUrl = "https://github.com/user/repo";
        String expectedContent = "# Public Repo README";

        when(responseSpec.body(String.class)).thenReturn(expectedContent);

        // Act
        String result = gitHubService.fetchReadme(repoUrl, null);

        // Assert
        assertThat(result).isEqualTo(expectedContent);
        verify(requestHeadersUriSpec, never()).header(eq("Authorization"), anyString());
    }
}
