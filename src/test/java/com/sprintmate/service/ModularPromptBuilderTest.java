package com.sprintmate.service;

import com.sprintmate.model.ProjectArchetype;
import com.sprintmate.model.ProjectTheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ModularPromptBuilder.
 * Tests that each prompt section is correctly generated from archetype, theme, and user data.
 */
class ModularPromptBuilderTest {

    private ModularPromptBuilder promptBuilder;
    private ProjectArchetype testArchetype;
    private ProjectTheme testTheme;

    @BeforeEach
    void setUp() {
        promptBuilder = new ModularPromptBuilder();

        testArchetype = ProjectArchetype.builder()
                .id(UUID.randomUUID())
                .code("CRUD_APP")
                .displayName("CRUD Application")
                .structureDescription("Standard CRUD app with forms and lists")
                .componentPatterns("CRUD,REST,Pagination")
                .apiPatterns("REST")
                .minComplexity(1)
                .maxComplexity(3)
                .build();

        testTheme = ProjectTheme.builder()
                .id(UUID.randomUUID())
                .code("finance")
                .displayName("Finance")
                .domainContext("Financial data and transactions")
                .exampleEntities("budget,transaction,account")
                .build();
    }

    @Test
    void should_IncludeAllSections_When_BuildingPrompt() {
        String prompt = promptBuilder.buildPrompt(
                Set.of("React", "TypeScript"),
                Set.of("Java", "Spring Boot"),
                testArchetype,
                testTheme,
                2,
                "WebSocket",
                "GraphQL"
        );

        assertThat(prompt).contains("creative tech mentor");
        assertThat(prompt).satisfiesAnyOf(
                p -> assertThat(p).contains("React, TypeScript"),
                p -> assertThat(p).contains("TypeScript, React")
        );
        assertThat(prompt).contains("CRUD Application");
        assertThat(prompt).contains("Finance");
        assertThat(prompt).contains("Intermediate");
        assertThat(prompt).contains("WebSocket");
        assertThat(prompt).contains("GraphQL");
        assertThat(prompt).contains("JSON");
    }

    @Test
    void should_UseDefaultSkills_When_SkillsAreEmpty() {
        String prompt = promptBuilder.buildPrompt(
                Set.of(),
                Set.of(),
                testArchetype,
                testTheme,
                2,
                null,
                null
        );

        assertThat(prompt).contains("React, TypeScript, CSS");
        assertThat(prompt).contains("Java, Spring Boot, PostgreSQL");
    }

    @Test
    void should_IncludeBeginnerGuidance_When_ComplexityIsOne() {
        String prompt = promptBuilder.buildPrompt(
                Set.of("React"),
                Set.of("Java"),
                testArchetype,
                testTheme,
                1,
                null,
                null
        );

        assertThat(prompt).contains("Beginner-Friendly");
        assertThat(prompt).contains("3-4 frontend tasks");
    }

    @Test
    void should_IncludeAdvancedGuidance_When_ComplexityIsThree() {
        String prompt = promptBuilder.buildPrompt(
                Set.of("React"),
                Set.of("Java"),
                testArchetype,
                testTheme,
                3,
                null,
                null
        );

        assertThat(prompt).contains("Advanced");
        assertThat(prompt).contains("5-7 frontend tasks");
    }

    @Test
    void should_OmitLearningGoals_When_BothAreNull() {
        String prompt = promptBuilder.buildPrompt(
                Set.of("React"),
                Set.of("Java"),
                testArchetype,
                testTheme,
                2,
                null,
                null
        );

        assertThat(prompt).doesNotContain("LEARNING GOALS");
    }

    @Test
    void should_IncludeLearningGoals_When_FrontendHasGoals() {
        String prompt = promptBuilder.buildPrompt(
                Set.of("React"),
                Set.of("Java"),
                testArchetype,
                testTheme,
                2,
                "WebSocket",
                null
        );

        assertThat(prompt).contains("LEARNING GOALS");
        assertThat(prompt).contains("Frontend developer wants to learn: WebSocket");
        assertThat(prompt).doesNotContain("Backend developer wants to learn");
    }

    @Test
    void should_IncludeArchetypeDetails_When_BuildingPrompt() {
        String prompt = promptBuilder.buildPrompt(
                Set.of("React"),
                Set.of("Java"),
                testArchetype,
                testTheme,
                2,
                null,
                null
        );

        assertThat(prompt).contains("CRUD Application");
        assertThat(prompt).contains("Standard CRUD app with forms and lists");
        assertThat(prompt).contains("CRUD,REST,Pagination");
    }

    @Test
    void should_IncludeThemeDetails_When_BuildingPrompt() {
        String prompt = promptBuilder.buildPrompt(
                Set.of("React"),
                Set.of("Java"),
                testArchetype,
                testTheme,
                2,
                null,
                null
        );

        assertThat(prompt).contains("Finance");
        assertThat(prompt).contains("Financial data and transactions");
        assertThat(prompt).contains("budget,transaction,account");
    }

    @Test
    void should_HandleNullArchetypePatterns_Gracefully() {
        ProjectArchetype minimalArchetype = ProjectArchetype.builder()
                .code("MINIMAL")
                .displayName("Minimal App")
                .structureDescription("Basic app")
                .componentPatterns(null)
                .apiPatterns(null)
                .build();

        String prompt = promptBuilder.buildPrompt(
                Set.of("React"),
                Set.of("Java"),
                minimalArchetype,
                testTheme,
                2,
                null,
                null
        );

        assertThat(prompt).contains("REST,CRUD");
        assertThat(prompt).contains("REST");
    }

    @Test
    void should_IncludeOutputFormat_When_BuildingPrompt() {
        String prompt = promptBuilder.buildPrompt(
                Set.of("React"),
                Set.of("Java"),
                testArchetype,
                testTheme,
                2,
                null,
                null
        );

        assertThat(prompt).contains("frontendTasks");
        assertThat(prompt).contains("backendTasks");
        assertThat(prompt).contains("apiEndpoints");
        assertThat(prompt).contains("title");
        assertThat(prompt).contains("description");
    }
}
