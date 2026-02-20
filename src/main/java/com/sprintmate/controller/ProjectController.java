package com.sprintmate.controller;

import com.sprintmate.dto.ProjectTemplateResponse;
import com.sprintmate.dto.ProjectThemeResponse;
import com.sprintmate.mapper.ProjectMapper;
import com.sprintmate.repository.ProjectThemeRepository;
import com.sprintmate.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Project Template operations.
 * 
 * Business Intent:
 * Provides endpoints for browsing available project templates.
 * Templates define the collaborative projects that matched pairs will work on.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project template endpoints")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    private final ProjectThemeRepository projectThemeRepository;

    /**
     * Retrieves all available project templates.
     * 
     * Business Intent:
     * Allows users to browse the catalog of available projects before matching.
     * Useful for understanding what types of projects are available on the platform.
     *
     * @return List of all project templates with their details
     */
    @GetMapping
    @Operation(
        summary = "Get all project templates",
        description = "Returns a list of all available project templates that matched pairs can work on."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Project templates retrieved successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProjectTemplateResponse.class)))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content
        )
    })
    public ResponseEntity<List<ProjectTemplateResponse>> getAllProjects() {
        var templates = projectService.getAllTemplates();
        var response = projectMapper.toResponseList(templates);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all active project themes.
     * Used by the frontend to populate the theme selector in profile editing.
     */
    @GetMapping("/themes")
    @Operation(
        summary = "Get all active project themes",
        description = "Returns available themes for user preference selection."
    )
    @ApiResponse(responseCode = "200", description = "Themes retrieved successfully")
    public ResponseEntity<List<ProjectThemeResponse>> getAvailableThemes() {
        var themes = projectThemeRepository.findByActiveTrue().stream()
                .map(t -> new ProjectThemeResponse(t.getCode(), t.getDisplayName()))
                .toList();
        return ResponseEntity.ok(themes);
    }
}
