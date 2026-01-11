package com.sprintmate.controller;

import com.sprintmate.dto.ProjectTemplateResponse;
import com.sprintmate.exception.GlobalExceptionHandler;
import com.sprintmate.mapper.ProjectMapper;
import com.sprintmate.model.ProjectTemplate;
import com.sprintmate.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ProjectController.
 * Tests HTTP layer for project template operations.
 */
@WebMvcTest(ProjectController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ProjectController Tests")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private ProjectMapper projectMapper;

    private OAuth2User mockOAuth2User;
    private List<ProjectTemplate> testTemplates;
    private List<ProjectTemplateResponse> testResponses;

    @BeforeEach
    void setUp() {
        // Create mock OAuth2User
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("login", "testuser");
        attributes.put("name", "Test User");
        mockOAuth2User = new DefaultOAuth2User(
            Collections.emptyList(),
            attributes,
            "login"
        );

        // Create test templates
        testTemplates = List.of(
            ProjectTemplate.builder()
                .id(UUID.randomUUID())
                .title("E-Commerce MVP")
                .description("Build a full-stack e-commerce application")
                .build(),
            ProjectTemplate.builder()
                .id(UUID.randomUUID())
                .title("Weather Dashboard")
                .description("Create a weather monitoring dashboard")
                .build()
        );

        testResponses = List.of(
            new ProjectTemplateResponse(
                testTemplates.get(0).getId(),
                "E-Commerce MVP",
                "Build a full-stack e-commerce application"
            ),
            new ProjectTemplateResponse(
                testTemplates.get(1).getId(),
                "Weather Dashboard",
                "Create a weather monitoring dashboard"
            )
        );
    }

    @Nested
    @DisplayName("GET /api/projects Tests")
    class GetAllProjectsTests {

        @Test
        @DisplayName("should_Return200_When_ProjectsExist")
        void should_Return200_When_ProjectsExist() throws Exception {
            // Arrange
            when(projectService.getAllTemplates()).thenReturn(testTemplates);
            when(projectMapper.toResponseList(testTemplates)).thenReturn(testResponses);

            // Act & Assert
            mockMvc.perform(get("/api/projects")
                    .with(oauth2Login().oauth2User(mockOAuth2User)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("E-Commerce MVP"))
                .andExpect(jsonPath("$[0].description").value("Build a full-stack e-commerce application"))
                .andExpect(jsonPath("$[1].title").value("Weather Dashboard"));
        }

        @Test
        @DisplayName("should_Return200_When_NoProjectsExist")
        void should_Return200_When_NoProjectsExist() throws Exception {
            // Arrange
            when(projectService.getAllTemplates()).thenReturn(Collections.emptyList());
            when(projectMapper.toResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/projects")
                    .with(oauth2Login().oauth2User(mockOAuth2User)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("should_Return302_When_NotAuthenticated")
        void should_Return302_When_NotAuthenticated() throws Exception {
            // Act & Assert - No oauth2Login() = unauthenticated
            mockMvc.perform(get("/api/projects"))
                .andExpect(status().isFound()); // 302 redirect to OAuth2 login
        }
    }
}
