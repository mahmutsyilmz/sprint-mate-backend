package com.sprintmate.service;

import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.model.ProjectTemplate;
import com.sprintmate.repository.ProjectTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProjectService.
 * Tests business logic for project template operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Tests")
class ProjectServiceTest {

    @Mock
    private ProjectTemplateRepository projectTemplateRepository;

    private ProjectService projectService;

    private List<ProjectTemplate> testTemplates;

    @BeforeEach
    void setUp() {
        testTemplates = List.of(
            ProjectTemplate.builder()
                .id(UUID.randomUUID())
                .title("E-Commerce MVP")
                .description("React + Java e-commerce application")
                .build(),
            ProjectTemplate.builder()
                .id(UUID.randomUUID())
                .title("Weather Dashboard")
                .description("Vue + Node weather application")
                .build(),
            ProjectTemplate.builder()
                .id(UUID.randomUUID())
                .title("Task Tracker")
                .description("Angular + Go task management application")
                .build()
        );
    }

    @Nested
    @DisplayName("getAllTemplates Tests")
    class GetAllTemplatesTests {

        @BeforeEach
        void setUp() {
            projectService = new ProjectService(projectTemplateRepository);
        }

        @Test
        @DisplayName("should_ReturnAllTemplates_When_TemplatesExist")
        void should_ReturnAllTemplates_When_TemplatesExist() {
            // Arrange
            when(projectTemplateRepository.findAll()).thenReturn(testTemplates);

            // Act
            List<ProjectTemplate> result = projectService.getAllTemplates();

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result).extracting(ProjectTemplate::getTitle)
                .containsExactly("E-Commerce MVP", "Weather Dashboard", "Task Tracker");
            
            verify(projectTemplateRepository).findAll();
        }

        @Test
        @DisplayName("should_ReturnEmptyList_When_NoTemplatesExist")
        void should_ReturnEmptyList_When_NoTemplatesExist() {
            // Arrange
            when(projectTemplateRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<ProjectTemplate> result = projectService.getAllTemplates();

            // Assert
            assertThat(result).isEmpty();
            verify(projectTemplateRepository).findAll();
        }
    }

    @Nested
    @DisplayName("getRandomTemplate Tests")
    class GetRandomTemplateTests {

        @Test
        @DisplayName("should_ReturnRandomTemplate_When_TemplatesExist")
        void should_ReturnRandomTemplate_When_TemplatesExist() {
            // Arrange - Use a fixed seed Random for predictable testing
            Random fixedRandom = new Random(42);
            projectService = new ProjectService(projectTemplateRepository, fixedRandom);
            when(projectTemplateRepository.findAll()).thenReturn(testTemplates);

            // Act
            ProjectTemplate result = projectService.getRandomTemplate();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isIn("E-Commerce MVP", "Weather Dashboard", "Task Tracker");
            verify(projectTemplateRepository).findAll();
        }

        @Test
        @DisplayName("should_ReturnDifferentTemplates_When_CalledMultipleTimes")
        void should_ReturnDifferentTemplates_When_CalledMultipleTimes() {
            // Arrange
            projectService = new ProjectService(projectTemplateRepository);
            when(projectTemplateRepository.findAll()).thenReturn(testTemplates);

            // Act - Call multiple times to verify randomness
            Set<String> selectedTitles = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                ProjectTemplate result = projectService.getRandomTemplate();
                selectedTitles.add(result.getTitle());
            }

            // Assert - With 100 calls, we should see variety (statistically likely)
            assertThat(selectedTitles.size()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("should_ThrowException_When_NoTemplatesExist")
        void should_ThrowException_When_NoTemplatesExist() {
            // Arrange
            projectService = new ProjectService(projectTemplateRepository);
            when(projectTemplateRepository.findAll()).thenReturn(Collections.emptyList());

            // Act & Assert
            assertThatThrownBy(() -> projectService.getRandomTemplate())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No project templates available");

            verify(projectTemplateRepository).findAll();
        }

        @Test
        @DisplayName("should_ReturnOnlyTemplate_When_SingleTemplateExists")
        void should_ReturnOnlyTemplate_When_SingleTemplateExists() {
            // Arrange
            ProjectTemplate singleTemplate = testTemplates.get(0);
            projectService = new ProjectService(projectTemplateRepository);
            when(projectTemplateRepository.findAll()).thenReturn(List.of(singleTemplate));

            // Act
            ProjectTemplate result = projectService.getRandomTemplate();

            // Assert
            assertThat(result).isEqualTo(singleTemplate);
            assertThat(result.getTitle()).isEqualTo("E-Commerce MVP");
        }

        @Test
        @DisplayName("should_SelectFromCorrectIndex_When_UsingControlledRandom")
        void should_SelectFromCorrectIndex_When_UsingControlledRandom() {
            // Arrange - Mock Random to always return index 1
            Random controlledRandom = mock(Random.class);
            when(controlledRandom.nextInt(3)).thenReturn(1);
            projectService = new ProjectService(projectTemplateRepository, controlledRandom);
            when(projectTemplateRepository.findAll()).thenReturn(testTemplates);

            // Act
            ProjectTemplate result = projectService.getRandomTemplate();

            // Assert - Should always get the second template (index 1)
            assertThat(result.getTitle()).isEqualTo("Weather Dashboard");
            verify(controlledRandom).nextInt(3);
        }
    }
}
