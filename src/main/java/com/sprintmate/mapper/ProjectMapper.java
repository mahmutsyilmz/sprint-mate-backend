package com.sprintmate.mapper;

import com.sprintmate.dto.ProjectTemplateResponse;
import com.sprintmate.model.ProjectTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between ProjectTemplate entity and DTOs.
 * 
 * Business Intent:
 * Centralizes entity-to-DTO conversion logic to ensure consistent
 * data transformation across all services that handle ProjectTemplate data.
 */
@Component
public class ProjectMapper {

    /**
     * Converts ProjectTemplate entity to ProjectTemplateResponse DTO.
     *
     * @param template The project template entity to convert
     * @return ProjectTemplateResponse DTO with template data
     */
    public ProjectTemplateResponse toResponse(ProjectTemplate template) {
        return new ProjectTemplateResponse(
            template.getId(),
            template.getTitle(),
            template.getDescription()
        );
    }

    /**
     * Converts a list of ProjectTemplate entities to a list of DTOs.
     *
     * @param templates List of project template entities
     * @return List of ProjectTemplateResponse DTOs
     */
    public List<ProjectTemplateResponse> toResponseList(List<ProjectTemplate> templates) {
        return templates.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
}
