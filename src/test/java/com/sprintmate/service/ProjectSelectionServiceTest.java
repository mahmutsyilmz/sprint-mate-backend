package com.sprintmate.service;

import com.sprintmate.model.*;
import com.sprintmate.repository.ProjectArchetypeRepository;
import com.sprintmate.repository.ProjectThemeRepository;
import com.sprintmate.repository.UserPreferenceRepository;
import com.sprintmate.service.ProjectSelectionService.SelectionResult;
import com.sprintmate.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProjectSelectionService.
 * Tests archetype/theme selection logic and preference intersection algorithm.
 */
@ExtendWith(MockitoExtension.class)
class ProjectSelectionServiceTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private ProjectArchetypeRepository archetypeRepository;

    @Mock
    private ProjectThemeRepository themeRepository;

    @InjectMocks
    private ProjectSelectionService projectSelectionService;

    private User frontendUser;
    private User backendUser;
    private ProjectArchetype crudArchetype;
    private ProjectArchetype realtimeArchetype;
    private ProjectTheme financeTheme;
    private ProjectTheme healthTheme;
    private ProjectTheme gamingTheme;

    @BeforeEach
    void setUp() {
        frontendUser = TestDataBuilder.buildUser(RoleName.FRONTEND);
        backendUser = TestDataBuilder.buildUser(RoleName.BACKEND);

        crudArchetype = ProjectArchetype.builder()
                .id(UUID.randomUUID())
                .code("CRUD_APP")
                .displayName("CRUD Application")
                .structureDescription("Standard CRUD app")
                .componentPatterns("CRUD,REST")
                .apiPatterns("REST")
                .minComplexity(1)
                .maxComplexity(3)
                .active(true)
                .build();

        realtimeArchetype = ProjectArchetype.builder()
                .id(UUID.randomUUID())
                .code("REAL_TIME_APP")
                .displayName("Real-Time Application")
                .structureDescription("WebSocket-based real-time app")
                .componentPatterns("WebSocket,Events")
                .apiPatterns("REST,WebSocket")
                .minComplexity(2)
                .maxComplexity(4)
                .active(true)
                .build();

        financeTheme = ProjectTheme.builder()
                .id(UUID.randomUUID())
                .code("finance")
                .displayName("Finance")
                .domainContext("Financial data and transactions")
                .exampleEntities("budget,transaction")
                .active(true)
                .build();

        healthTheme = ProjectTheme.builder()
                .id(UUID.randomUUID())
                .code("health")
                .displayName("Health")
                .domainContext("Health and wellness tracking")
                .exampleEntities("patient,appointment")
                .active(true)
                .build();

        gamingTheme = ProjectTheme.builder()
                .id(UUID.randomUUID())
                .code("gaming")
                .displayName("Gaming")
                .domainContext("Games and interactive entertainment")
                .exampleEntities("player,score")
                .active(true)
                .build();
    }

    // --- Complexity Calculation Tests ---

    @Test
    void should_ReturnDefaultComplexity_When_BothPreferencesAreNull() {
        int result = projectSelectionService.calculateComplexity(null, null);
        assertThat(result).isEqualTo(2);
    }

    @Test
    void should_ReturnFrontendDifficulty_When_OnlyFrontendHasPreference() {
        UserPreference fePref = UserPreference.builder().difficultyPreference(3).build();
        int result = projectSelectionService.calculateComplexity(fePref, null);
        assertThat(result).isEqualTo(3);
    }

    @Test
    void should_ReturnBackendDifficulty_When_OnlyBackendHasPreference() {
        UserPreference bePref = UserPreference.builder().difficultyPreference(1).build();
        int result = projectSelectionService.calculateComplexity(null, bePref);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void should_ReturnAverage_When_BothHavePreferences() {
        UserPreference fePref = UserPreference.builder().difficultyPreference(1).build();
        UserPreference bePref = UserPreference.builder().difficultyPreference(3).build();
        int result = projectSelectionService.calculateComplexity(fePref, bePref);
        assertThat(result).isEqualTo(2);
    }

    @Test
    void should_RoundUpAverage_When_AverageIsNotWhole() {
        UserPreference fePref = UserPreference.builder().difficultyPreference(2).build();
        UserPreference bePref = UserPreference.builder().difficultyPreference(3).build();
        int result = projectSelectionService.calculateComplexity(fePref, bePref);
        // (2+3)/2.0 = 2.5 → rounds to 3
        assertThat(result).isEqualTo(3);
    }

    @Test
    void should_ReturnDefault_When_PreferencesExistButDifficultyIsNull() {
        UserPreference fePref = UserPreference.builder().build();
        UserPreference bePref = UserPreference.builder().build();
        int result = projectSelectionService.calculateComplexity(fePref, bePref);
        assertThat(result).isEqualTo(2);
    }

    // --- Archetype Selection Tests ---

    @Test
    void should_SelectArchetype_When_MatchingComplexityExists() {
        when(archetypeRepository.findByComplexityLevel(2)).thenReturn(List.of(crudArchetype, realtimeArchetype));

        ProjectArchetype result = projectSelectionService.selectArchetype(2);
        assertThat(result).isIn(crudArchetype, realtimeArchetype);
    }

    @Test
    void should_FallbackToWidenedRange_When_NoExactComplexityMatch() {
        when(archetypeRepository.findByComplexityLevel(5)).thenReturn(List.of());
        when(archetypeRepository.findByActiveTrue()).thenReturn(List.of(crudArchetype, realtimeArchetype));

        // realtimeArchetype has maxComplexity=4, which is >= lower bound (4)
        ProjectArchetype result = projectSelectionService.selectArchetype(5);
        assertThat(result).isEqualTo(realtimeArchetype);
    }

    @Test
    void should_FallbackToAllActive_When_NoArchetypeMatchesAtAll() {
        when(archetypeRepository.findByComplexityLevel(5)).thenReturn(List.of());
        // Return empty for widened range filter
        ProjectArchetype extremeArchetype = ProjectArchetype.builder()
                .code("EXTREME")
                .minComplexity(1)
                .maxComplexity(1)
                .active(true)
                .build();
        when(archetypeRepository.findByActiveTrue()).thenReturn(List.of(extremeArchetype));

        // Complexity 5, widened to 4-5. extremeArchetype (1-1) doesn't match widened range.
        // First findByActiveTrue call for widened filter returns empty match.
        // Second findByActiveTrue call returns extremeArchetype as final fallback.
        ProjectArchetype result = projectSelectionService.selectArchetype(5);
        assertThat(result).isNotNull();
    }

    // --- Theme Selection Tests ---

    @Test
    void should_SelectFromIntersection_When_BothUsersShareThemes() {
        Set<ProjectTheme> feThemes = Set.of(financeTheme, healthTheme);
        Set<ProjectTheme> beThemes = Set.of(financeTheme, gamingTheme);

        UserPreference fePref = UserPreference.builder().preferredThemes(feThemes).build();
        UserPreference bePref = UserPreference.builder().preferredThemes(beThemes).build();

        when(themeRepository.findByCodeIn(Set.of("finance"))).thenReturn(List.of(financeTheme));

        ProjectTheme result = projectSelectionService.selectTheme(fePref, bePref);
        assertThat(result).isEqualTo(financeTheme);
    }

    @Test
    void should_SelectFromUnion_When_NoIntersectionExists() {
        Set<ProjectTheme> feThemes = Set.of(healthTheme);
        Set<ProjectTheme> beThemes = Set.of(gamingTheme);

        UserPreference fePref = UserPreference.builder().preferredThemes(feThemes).build();
        UserPreference bePref = UserPreference.builder().preferredThemes(beThemes).build();

        when(themeRepository.findByCodeIn(Set.of("health", "gaming")))
                .thenReturn(List.of(healthTheme, gamingTheme));

        ProjectTheme result = projectSelectionService.selectTheme(fePref, bePref);
        assertThat(result).isIn(healthTheme, gamingTheme);
    }

    @Test
    void should_SelectFromAllActive_When_NoPreferencesExist() {
        when(themeRepository.findByActiveTrue()).thenReturn(List.of(financeTheme, healthTheme, gamingTheme));

        ProjectTheme result = projectSelectionService.selectTheme(null, null);
        assertThat(result).isIn(financeTheme, healthTheme, gamingTheme);
    }

    @Test
    void should_UseUnion_When_OnlyOneUserHasThemePreferences() {
        Set<ProjectTheme> feThemes = Set.of(financeTheme);
        UserPreference fePref = UserPreference.builder().preferredThemes(feThemes).build();

        when(themeRepository.findByCodeIn(Set.of("finance"))).thenReturn(List.of(financeTheme));

        ProjectTheme result = projectSelectionService.selectTheme(fePref, null);
        assertThat(result).isEqualTo(financeTheme);
    }

    @Test
    void should_FallbackToActive_When_PreferencesExistButThemesEmpty() {
        UserPreference fePref = UserPreference.builder().preferredThemes(new HashSet<>()).build();
        UserPreference bePref = UserPreference.builder().preferredThemes(new HashSet<>()).build();

        when(themeRepository.findByActiveTrue()).thenReturn(List.of(financeTheme));

        ProjectTheme result = projectSelectionService.selectTheme(fePref, bePref);
        assertThat(result).isEqualTo(financeTheme);
    }

    // --- Full Selection Integration ---

    @Test
    void should_ReturnCompleteResult_When_BothUsersHavePreferences() {
        UserPreference fePref = UserPreference.builder()
                .difficultyPreference(2)
                .learningGoals("WebSocket")
                .preferredThemes(Set.of(financeTheme))
                .build();

        UserPreference bePref = UserPreference.builder()
                .difficultyPreference(2)
                .learningGoals("GraphQL")
                .preferredThemes(Set.of(financeTheme, healthTheme))
                .build();

        when(userPreferenceRepository.findByUserId(frontendUser.getId())).thenReturn(Optional.of(fePref));
        when(userPreferenceRepository.findByUserId(backendUser.getId())).thenReturn(Optional.of(bePref));
        when(archetypeRepository.findByComplexityLevel(2)).thenReturn(List.of(crudArchetype));
        when(themeRepository.findByCodeIn(Set.of("finance"))).thenReturn(List.of(financeTheme));

        SelectionResult result = projectSelectionService.select(frontendUser, backendUser);

        assertThat(result).isNotNull();
        assertThat(result.archetype()).isEqualTo(crudArchetype);
        assertThat(result.theme()).isEqualTo(financeTheme);
        assertThat(result.targetComplexity()).isEqualTo(2);
        assertThat(result.frontendLearningGoals()).isEqualTo("WebSocket");
        assertThat(result.backendLearningGoals()).isEqualTo("GraphQL");
    }

    @Test
    void should_UseDefaults_When_NeitherUserHasPreferences() {
        when(userPreferenceRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(archetypeRepository.findByComplexityLevel(2)).thenReturn(List.of(crudArchetype));
        when(themeRepository.findByActiveTrue()).thenReturn(List.of(financeTheme));

        SelectionResult result = projectSelectionService.select(frontendUser, backendUser);

        assertThat(result).isNotNull();
        assertThat(result.targetComplexity()).isEqualTo(2);
        assertThat(result.frontendLearningGoals()).isNull();
        assertThat(result.backendLearningGoals()).isNull();
    }
}
