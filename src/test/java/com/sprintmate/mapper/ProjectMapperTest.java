package com.sprintmate.mapper;

import com.sprintmate.dto.ProjectTemplateResponse;
import com.sprintmate.model.ProjectTemplate;
import com.sprintmate.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ProjectMapper.
 * Tests entity-to-DTO conversion logic for project templates.
 */
class ProjectMapperTest {

    private ProjectMapper projectMapper;

    @BeforeEach
    void setUp() {
        projectMapper = new ProjectMapper();
    }

    @Test
    void should_MapTemplateToResponse_When_AllFieldsPresent() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        ProjectTemplate template = ProjectTemplate.builder()
                .id(templateId)
                .title("E-commerce Dashboard")
                .description("Build a responsive admin dashboard for e-commerce")
                .build();

        // Act
        ProjectTemplateResponse response = projectMapper.toResponse(template);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(templateId);
        assertThat(response.title()).isEqualTo("E-commerce Dashboard");
        assertThat(response.description()).isEqualTo("Build a responsive admin dashboard for e-commerce");
    }

    @Test
    void should_HandleNullDescription_When_MappingTemplate() {
        // Arrange
        ProjectTemplate template = ProjectTemplate.builder()
                .id(UUID.randomUUID())
                .title("Simple Project")
                .description(null)
                .build();

        // Act
        ProjectTemplateResponse response = projectMapper.toResponse(template);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Simple Project");
        assertThat(response.description()).isNull();
    }

    @Test
    void should_MapEmptyListToEmptyResponse_When_NoTemplates() {
        // Arrange
        List<ProjectTemplate> templates = List.of();

        // Act
        List<ProjectTemplateResponse> responses = projectMapper.toResponseList(templates);

        // Assert
        assertThat(responses).isNotNull();
        assertThat(responses).isEmpty();
    }

    @Test
    void should_MapMultipleTemplatesToResponses_When_ListProvided() {
        // Arrange
        ProjectTemplate template1 = TestDataBuilder.buildProjectTemplate(
                "Todo App", "Build a task management app");
        ProjectTemplate template2 = TestDataBuilder.buildProjectTemplate(
                "Weather Dashboard", "Real-time weather visualization");
        ProjectTemplate template3 = TestDataBuilder.buildProjectTemplate(
                "Chat App", "WebSocket-based real-time chat");

        List<ProjectTemplate> templates = List.of(template1, template2, template3);

        // Act
        List<ProjectTemplateResponse> responses = projectMapper.toResponseList(templates);

        // Assert
        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).title()).isEqualTo("Todo App");
        assertThat(responses.get(1).title()).isEqualTo("Weather Dashboard");
        assertThat(responses.get(2).title()).isEqualTo("Chat App");
    }

    @Test
    void should_PreserveDataIntegrity_When_MappingList() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ProjectTemplate template1 = ProjectTemplate.builder()
                .id(id1)
                .title("Project 1")
                .description("Description 1")
                .build();

        ProjectTemplate template2 = ProjectTemplate.builder()
                .id(id2)
                .title("Project 2")
                .description("Description 2")
                .build();

        List<ProjectTemplate> templates = List.of(template1, template2);

        // Act
        List<ProjectTemplateResponse> responses = projectMapper.toResponseList(templates);

        // Assert
        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).id()).isEqualTo(id1);
        assertThat(responses.get(0).title()).isEqualTo("Project 1");
        assertThat(responses.get(0).description()).isEqualTo("Description 1");

        assertThat(responses.get(1).id()).isEqualTo(id2);
        assertThat(responses.get(1).title()).isEqualTo("Project 2");
        assertThat(responses.get(1).description()).isEqualTo("Description 2");
    }
}
