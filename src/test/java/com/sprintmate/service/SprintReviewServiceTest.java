package com.sprintmate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintmate.model.*;
import com.sprintmate.repository.MatchProjectRepository;
import com.sprintmate.repository.SprintReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SprintReviewService.
 * Tests AI-powered sprint review generation and fallback behavior.
 *
 * Validates Bug #9 fix: RestClient is now injected via RestClient.Builder
 * instead of being created directly in the constructor.
 */
@ExtendWith(MockitoExtension.class)
class SprintReviewServiceTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private GitHubService gitHubService;

    @Mock
    private SprintReviewRepository sprintReviewRepository;

    @Mock
    private MatchProjectRepository matchProjectRepository;

    @Mock
    private ObjectMapper objectMapper;

    private SprintReviewService sprintReviewService;

    private Match match;
    private String repoUrl;

    @BeforeEach
    void setUp() {
        // Setup fluent RestClient mock chain (lenient to avoid UnnecessaryStubbingException)
        lenient().when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.build()).thenReturn(restClient);
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.body(anyString())).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        sprintReviewService = new SprintReviewService(
                "test-api-key",
                "https://api.groq.com",
                restClientBuilder,
                gitHubService,
                sprintReviewRepository,
                matchProjectRepository,
                objectMapper
        );

        match = Match.builder()
                .id(UUID.randomUUID())
                .status(MatchStatus.ACTIVE)
                .build();

        repoUrl = "https://github.com/team/project";
    }

    @Test
    void should_CreateEmptyReview_When_ReadmeIsBlank() {
        // Arrange
        SprintReview emptyReview = SprintReview.builder()
                .match(match)
                .repoUrl(repoUrl)
                .score(0)
                .aiFeedback("No README content found")
                .strengths("[]")
                .missingElements("[]")
                .readmeContent(null)
                .build();

        when(gitHubService.fetchReadme(eq(repoUrl), any())).thenReturn("");
        when(sprintReviewRepository.save(any(SprintReview.class))).thenReturn(emptyReview);

        // Act
        Optional<SprintReview> result = sprintReviewService.generateReview(match, repoUrl, null);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getScore()).isEqualTo(0);
        assertThat(result.get().getAiFeedback()).contains("No README content found");
        verify(matchProjectRepository, never()).findByMatch(any());
    }

    @Test
    void should_CreateEmptyReview_When_NoMatchProjectFound() {
        // Arrange
        String readmeContent = "# My Project\n\nProject description";

        SprintReview emptyReview = SprintReview.builder()
                .match(match)
                .repoUrl(repoUrl)
                .score(0)
                .aiFeedback("No project context available")
                .strengths("[]")
                .missingElements("[]")
                .readmeContent(null)
                .build();

        when(gitHubService.fetchReadme(eq(repoUrl), any())).thenReturn(readmeContent);
        when(matchProjectRepository.findByMatch(match)).thenReturn(Optional.empty());
        when(sprintReviewRepository.save(any(SprintReview.class))).thenReturn(emptyReview);

        // Act
        Optional<SprintReview> result = sprintReviewService.generateReview(match, repoUrl, null);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getScore()).isEqualTo(0);
        assertThat(result.get().getAiFeedback()).contains("No project context available");
    }

    @Test
    void should_CreateBasicReview_When_NoContextAvailable() {
        // Arrange
        String readmeContent = "# Project README\n\nThis is our completed project.";

        MatchProject matchProject = MatchProject.builder()
                .match(match)
                .projectPromptContext(null) // No context available
                .build();

        SprintReview basicReview = SprintReview.builder()
                .match(match)
                .repoUrl(repoUrl)
                .score(50)
                .aiFeedback("Review completed without crisis context comparison")
                .strengths("[]")
                .missingElements("[]")
                .readmeContent(readmeContent)
                .build();

        when(gitHubService.fetchReadme(eq(repoUrl), any())).thenReturn(readmeContent);
        when(matchProjectRepository.findByMatch(match)).thenReturn(Optional.of(matchProject));
        when(sprintReviewRepository.save(any(SprintReview.class))).thenReturn(basicReview);

        // Act
        Optional<SprintReview> result = sprintReviewService.generateReview(match, repoUrl, null);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getScore()).isEqualTo(50);
        assertThat(result.get().getAiFeedback()).contains("without crisis context");
        verify(sprintReviewRepository).save(any(SprintReview.class));
    }

    @Test
    void should_HandleGitHubServiceFailure_When_ReadmeFetchFails() {
        // Arrange
        SprintReview emptyReview = SprintReview.builder()
                .match(match)
                .repoUrl(repoUrl)
                .score(0)
                .aiFeedback("Review generation failed")
                .strengths("[]")
                .missingElements("[]")
                .readmeContent(null)
                .build();

        when(gitHubService.fetchReadme(eq(repoUrl), any())).thenThrow(new RuntimeException("GitHub API error"));
        when(sprintReviewRepository.save(any(SprintReview.class))).thenReturn(emptyReview);

        // Act
        Optional<SprintReview> result = sprintReviewService.generateReview(match, repoUrl, null);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getScore()).isEqualTo(0);
    }

    @Test
    void should_TruncateReadme_When_ContentTooLong() {
        // Arrange
        String longReadme = "x".repeat(10000); // Longer than 9000 char limit

        MatchProject matchProject = MatchProject.builder()
                .match(match)
                .projectPromptContext(null)
                .build();

        when(gitHubService.fetchReadme(eq(repoUrl), any())).thenReturn(longReadme);
        when(matchProjectRepository.findByMatch(match)).thenReturn(Optional.of(matchProject));
        when(sprintReviewRepository.save(any(SprintReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sprintReviewService.generateReview(match, repoUrl, null);

        // Assert
        ArgumentCaptor<SprintReview> reviewCaptor = ArgumentCaptor.forClass(SprintReview.class);
        verify(sprintReviewRepository).save(reviewCaptor.capture());

        SprintReview savedReview = reviewCaptor.getValue();
        assertThat(savedReview.getReadmeContent()).hasSize(9000 + "\n... [truncated]".length());
        assertThat(savedReview.getReadmeContent()).contains("[truncated]");
    }

    @Test
    void should_ValidateRestClientInjection_When_ServiceConstructed() {
        // Validates Bug #9 fix: RestClient.Builder is injected

        // Assert
        verify(restClientBuilder).baseUrl("https://api.groq.com");
        verify(restClientBuilder, times(2)).defaultHeader(anyString(), anyString());
        verify(restClientBuilder).build();
    }

    @Test
    void should_ReturnEmptyOptional_When_EmptyReviewSaveFails() {
        // Arrange
        when(gitHubService.fetchReadme(eq(repoUrl), any())).thenReturn("");
        when(sprintReviewRepository.save(any(SprintReview.class))).thenThrow(new RuntimeException("Database error"));

        // Act
        Optional<SprintReview> result = sprintReviewService.generateReview(match, repoUrl, null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_ReturnEmptyOptional_When_BasicReviewSaveFails() {
        // Arrange
        String readmeContent = "# Project";

        MatchProject matchProject = MatchProject.builder()
                .match(match)
                .projectPromptContext(null)
                .build();

        when(gitHubService.fetchReadme(eq(repoUrl), any())).thenReturn(readmeContent);
        when(matchProjectRepository.findByMatch(match)).thenReturn(Optional.of(matchProject));
        when(sprintReviewRepository.save(any(SprintReview.class))).thenThrow(new RuntimeException("Database error"));

        // Act
        Optional<SprintReview> result = sprintReviewService.generateReview(match, repoUrl, null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_HandleNullReadme_When_GitHubReturnsNull() {
        // Arrange
        SprintReview emptyReview = SprintReview.builder()
                .match(match)
                .repoUrl(repoUrl)
                .score(0)
                .aiFeedback("No README content found")
                .strengths("[]")
                .missingElements("[]")
                .readmeContent(null)
                .build();

        when(gitHubService.fetchReadme(eq(repoUrl), any())).thenReturn(null);
        when(sprintReviewRepository.save(any(SprintReview.class))).thenReturn(emptyReview);

        // Act
        Optional<SprintReview> result = sprintReviewService.generateReview(match, repoUrl, null);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getScore()).isEqualTo(0);
    }

    @Test
    void should_AttemptAIReview_When_ContextAvailable() {
        // Arrange
        String readmeContent = "# Our Solution\n\nWe implemented the crisis solution.";

        ProjectPromptContext context = ProjectPromptContext.builder()
                .industry("Finance")
                .subDomain("Banking")
                .crisisCategory("System Outage")
                .crisisScenario("Critical payment system down")
                .primaryConstraint("2-hour recovery time")
                .secondaryConstraint("Zero data loss")
                .complianceRequirement("PCI-DSS")
                .successMetric("System restored with full transaction history")
                .timeline("1 week")
                .build();

        MatchProject matchProject = MatchProject.builder()
                .match(match)
                .projectPromptContext(context)
                .build();

        SprintReview fallbackReview = SprintReview.builder()
                .match(match)
                .repoUrl(repoUrl)
                .score(0)
                .aiFeedback("AI analysis failed")
                .strengths("[]")
                .missingElements("[]")
                .readmeContent(null)
                .build();

        when(gitHubService.fetchReadme(eq(repoUrl), any())).thenReturn(readmeContent);
        when(matchProjectRepository.findByMatch(match)).thenReturn(Optional.of(matchProject));
        when(sprintReviewRepository.save(any(SprintReview.class))).thenReturn(fallbackReview);

        // Act
        Optional<SprintReview> result = sprintReviewService.generateReview(match, repoUrl, null);

        // Assert
        assertThat(result).isPresent();
        // Should attempt AI review but fall back on error
        verify(gitHubService).fetchReadme(eq(repoUrl), any());
        verify(matchProjectRepository).findByMatch(match);
    }

    @Test
    void should_SaveReviewWithCorrectFields_When_BasicReviewCreated() {
        // Arrange
        String readmeContent = "# My README\n\nProject details";

        MatchProject matchProject = MatchProject.builder()
                .match(match)
                .projectPromptContext(null)
                .build();

        when(gitHubService.fetchReadme(eq(repoUrl), any())).thenReturn(readmeContent);
        when(matchProjectRepository.findByMatch(match)).thenReturn(Optional.of(matchProject));
        when(sprintReviewRepository.save(any(SprintReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sprintReviewService.generateReview(match, repoUrl, null);

        // Assert
        ArgumentCaptor<SprintReview> reviewCaptor = ArgumentCaptor.forClass(SprintReview.class);
        verify(sprintReviewRepository).save(reviewCaptor.capture());

        SprintReview savedReview = reviewCaptor.getValue();
        assertThat(savedReview.getMatch()).isEqualTo(match);
        assertThat(savedReview.getRepoUrl()).isEqualTo(repoUrl);
        assertThat(savedReview.getScore()).isEqualTo(50);
        assertThat(savedReview.getStrengths()).isEqualTo("[]");
        assertThat(savedReview.getMissingElements()).isEqualTo("[]");
        assertThat(savedReview.getReadmeContent()).isEqualTo(readmeContent);
    }

    @Test
    void should_SaveReviewWithZeroScore_When_EmptyReviewCreated() {
        // Arrange
        when(gitHubService.fetchReadme(eq(repoUrl), any())).thenReturn("");
        when(sprintReviewRepository.save(any(SprintReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sprintReviewService.generateReview(match, repoUrl, null);

        // Assert
        ArgumentCaptor<SprintReview> reviewCaptor = ArgumentCaptor.forClass(SprintReview.class);
        verify(sprintReviewRepository).save(reviewCaptor.capture());

        SprintReview savedReview = reviewCaptor.getValue();
        assertThat(savedReview.getScore()).isEqualTo(0);
        assertThat(savedReview.getReadmeContent()).isNull();
    }
}
