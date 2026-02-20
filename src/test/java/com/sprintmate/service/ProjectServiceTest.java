package com.sprintmate.service;

import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.model.ProjectTemplate;
import com.sprintmate.repository.ProjectTemplateRepository;
import com.sprintmate.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProjectService.
 * Tests project template retrieval and random selection logic.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectTemplateRepository projectTemplateRepository;

    @Mock
    private Random random;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectTemplateRepository, random);
    }

    @Test
    void should_ReturnAllTemplates_When_TemplatesExist() {
        // Arrange
        List<ProjectTemplate> templates = List.of(
                TestDataBuilder.buildProjectTemplate("Todo App", "Build a task manager"),
                TestDataBuilder.buildProjectTemplate("Weather Dashboard", "Real-time weather"),
                TestDataBuilder.buildProjectTemplate("Chat App", "WebSocket chat")
        );

        when(projectTemplateRepository.findAll()).thenReturn(templates);

        // Act
        List<ProjectTemplate> result = projectService.getAllTemplates();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getTitle()).isEqualTo("Todo App");
        assertThat(result.get(1).getTitle()).isEqualTo("Weather Dashboard");
        assertThat(result.get(2).getTitle()).isEqualTo("Chat App");

        verify(projectTemplateRepository).findAll();
    }

    @Test
    void should_ReturnEmptyList_When_NoTemplates() {
        // Arrange
        when(projectTemplateRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<ProjectTemplate> result = projectService.getAllTemplates();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(projectTemplateRepository).findAll();
    }

    @Test
    void should_ReturnRandomTemplate_When_TemplatesExist() {
        // Arrange
        List<ProjectTemplate> templates = List.of(
                TestDataBuilder.buildProjectTemplate("Project 1", "Description 1"),
                TestDataBuilder.buildProjectTemplate("Project 2", "Description 2"),
                TestDataBuilder.buildProjectTemplate("Project 3", "Description 3")
        );

        when(projectTemplateRepository.findAll()).thenReturn(templates);
        when(random.nextInt(3)).thenReturn(1); // Select second template

        // Act
        ProjectTemplate result = projectService.getRandomTemplate();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Project 2");

        verify(projectTemplateRepository).findAll();
        verify(random).nextInt(3);
    }

    @Test
    void should_ThrowException_When_NoTemplatesAvailable() {
        // Arrange
        when(projectTemplateRepository.findAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> projectService.getRandomTemplate())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No project templates available");

        verify(projectTemplateRepository).findAll();
        verify(random, never()).nextInt(anyInt());
    }

    @Test
    void should_ReturnFirstTemplate_When_RandomReturns0() {
        // Arrange
        List<ProjectTemplate> templates = List.of(
                TestDataBuilder.buildProjectTemplate("First", "First project"),
                TestDataBuilder.buildProjectTemplate("Second", "Second project")
        );

        when(projectTemplateRepository.findAll()).thenReturn(templates);
        when(random.nextInt(2)).thenReturn(0);

        // Act
        ProjectTemplate result = projectService.getRandomTemplate();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("First");
    }

    @Test
    void should_ReturnLastTemplate_When_RandomReturnsLastIndex() {
        // Arrange
        List<ProjectTemplate> templates = List.of(
                TestDataBuilder.buildProjectTemplate("First", "First project"),
                TestDataBuilder.buildProjectTemplate("Second", "Second project"),
                TestDataBuilder.buildProjectTemplate("Last", "Last project")
        );

        when(projectTemplateRepository.findAll()).thenReturn(templates);
        when(random.nextInt(3)).thenReturn(2); // Last index

        // Act
        ProjectTemplate result = projectService.getRandomTemplate();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Last");
    }

    @Test
    void should_SelectCorrectTemplate_When_RandomIndexProvided() {
        // Arrange
        List<ProjectTemplate> templates = List.of(
                TestDataBuilder.buildProjectTemplate("A", "Description A"),
                TestDataBuilder.buildProjectTemplate("B", "Description B"),
                TestDataBuilder.buildProjectTemplate("C", "Description C"),
                TestDataBuilder.buildProjectTemplate("D", "Description D"),
                TestDataBuilder.buildProjectTemplate("E", "Description E")
        );

        when(projectTemplateRepository.findAll()).thenReturn(templates);
        when(random.nextInt(5)).thenReturn(3); // Select index 3 (fourth template)

        // Act
        ProjectTemplate result = projectService.getRandomTemplate();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("D");
        assertThat(result.getDescription()).isEqualTo("Description D");
    }

    @Test
    void should_HandleSingleTemplate_When_OnlyOneExists() {
        // Arrange
        List<ProjectTemplate> templates = List.of(
                TestDataBuilder.buildProjectTemplate("Only One", "The only template")
        );

        when(projectTemplateRepository.findAll()).thenReturn(templates);
        when(random.nextInt(1)).thenReturn(0);

        // Act
        ProjectTemplate result = projectService.getRandomTemplate();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Only One");

        verify(random).nextInt(1); // Should call with size 1
    }
}
