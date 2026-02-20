package com.sprintmate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintmate.model.*;
import com.sprintmate.repository.ProjectTemplateRepository;
import com.sprintmate.service.ProjectGeneratorService.GeneratedProject;
import com.sprintmate.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GroqProjectGenerator.
 * Tests AI-driven project generation logic with archetype+theme system.
 */
@ExtendWith(MockitoExtension.class)
class GroqProjectGeneratorTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private ProjectTemplateRepository projectTemplateRepository;

    @Mock
    private ProjectSelectionService projectSelectionService;

    @Mock
    private ModularPromptBuilder modularPromptBuilder;

    @Mock
    private ObjectMapper objectMapper;

    private GroqProjectGenerator groqProjectGenerator;

    private User frontendUser;
    private User backendUser;
    private ProjectArchetype testArchetype;
    private ProjectTheme testTheme;

    @BeforeEach
    void setUp() {
        // Setup fluent RestClient mock chain (lenient to avoid UnnecessaryStubbingException)
        lenient().when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.build()).thenReturn(restClient);
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.body(anyString())).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        groqProjectGenerator = new GroqProjectGenerator(
                "test-api-key",
                "https://api.groq.com",
                restClientBuilder,
                projectTemplateRepository,
                projectSelectionService,
                modularPromptBuilder,
                objectMapper
        );

        // Setup test users
        frontendUser = TestDataBuilder.buildUser(RoleName.FRONTEND);
        frontendUser.setSkills(Set.of("React", "TypeScript", "CSS"));

        backendUser = TestDataBuilder.buildUser(RoleName.BACKEND);
        backendUser.setSkills(Set.of("Java", "Spring Boot", "PostgreSQL"));

        // Setup test archetype and theme
        testArchetype = ProjectArchetype.builder()
                .code("CRUD_APP")
                .displayName("CRUD Application")
                .structureDescription("Standard CRUD app")
                .componentPatterns("CRUD,REST")
                .apiPatterns("REST")
                .minComplexity(1)
                .maxComplexity(3)
                .build();

        testTheme = ProjectTheme.builder()
                .code("finance")
                .displayName("Finance")
                .domainContext("Financial data and transactions")
                .exampleEntities("budget,transaction")
                .build();
    }

    @Test
    void should_UseFallbackTemplate_When_GroqApiFails() {
        // Arrange
        ProjectSelectionService.SelectionResult selection =
                new ProjectSelectionService.SelectionResult(testArchetype, testTheme, 2, null, null);

        when(projectSelectionService.select(frontendUser, backendUser)).thenReturn(selection);
        when(modularPromptBuilder.buildPrompt(any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn("test prompt");
        when(restClient.post()).thenThrow(new RuntimeException("Groq API error"));

        ProjectTemplate fallbackTemplate = ProjectTemplate.builder()
                .title("Collaborative Mini Project")
                .description("Fallback project description")
                .build();
        when(projectTemplateRepository.save(any(ProjectTemplate.class))).thenReturn(fallbackTemplate);

        // Act
        GeneratedProject result = groqProjectGenerator.generateProject(frontendUser, backendUser, "Any Topic");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.template()).isNotNull();
        assertThat(result.archetype()).isNull();
        assertThat(result.theme()).isNull();
        verify(projectTemplateRepository).save(any(ProjectTemplate.class));
    }

    @Test
    void should_CallSelectionService_When_GeneratingProject() {
        // Arrange
        ProjectSelectionService.SelectionResult selection =
                new ProjectSelectionService.SelectionResult(testArchetype, testTheme, 2, "WebSocket", "GraphQL");

        when(projectSelectionService.select(frontendUser, backendUser)).thenReturn(selection);
        when(modularPromptBuilder.buildPrompt(any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn("test prompt");

        ProjectTemplate fallbackTemplate = ProjectTemplate.builder()
                .title("Test")
                .description("Test")
                .build();
        when(restClient.post()).thenThrow(new RuntimeException("API error"));
        when(projectTemplateRepository.save(any(ProjectTemplate.class))).thenReturn(fallbackTemplate);

        // Act
        groqProjectGenerator.generateProject(frontendUser, backendUser, "Social");

        // Assert
        verify(projectSelectionService).select(frontendUser, backendUser);
        verify(modularPromptBuilder).buildPrompt(
                eq(frontendUser.getSkills()),
                eq(backendUser.getSkills()),
                eq(testArchetype),
                eq(testTheme),
                eq(2),
                eq("WebSocket"),
                eq("GraphQL")
        );
    }

    @Test
    void should_HandleEmptySkills_When_UsersHaveNoSkills() {
        // Arrange
        frontendUser.setSkills(new HashSet<>());
        backendUser.setSkills(new HashSet<>());

        ProjectSelectionService.SelectionResult selection =
                new ProjectSelectionService.SelectionResult(testArchetype, testTheme, 2, null, null);

        when(projectSelectionService.select(frontendUser, backendUser)).thenReturn(selection);
        when(modularPromptBuilder.buildPrompt(any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn("test prompt");

        ProjectTemplate fallbackTemplate = ProjectTemplate.builder()
                .title("Default Project")
                .description("Default")
                .build();
        when(restClient.post()).thenThrow(new RuntimeException("API error"));
        when(projectTemplateRepository.save(any(ProjectTemplate.class))).thenReturn(fallbackTemplate);

        // Act
        GeneratedProject result = groqProjectGenerator.generateProject(frontendUser, backendUser, "Default");

        // Assert
        assertThat(result).isNotNull();
        verify(projectTemplateRepository).save(any(ProjectTemplate.class));
    }

    @Test
    void should_ValidateRestClientInjection_When_ServiceConstructed() {
        // Validates RestClient.Builder is injected
        verify(restClientBuilder).baseUrl("https://api.groq.com");
        verify(restClientBuilder, times(2)).defaultHeader(anyString(), anyString());
        verify(restClientBuilder).build();
    }
}
