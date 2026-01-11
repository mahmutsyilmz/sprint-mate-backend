package com.sprintmate.service;

import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.model.ProjectTemplate;
import com.sprintmate.repository.ProjectTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

/**
 * Service for managing project templates.
 * Provides operations for retrieving and selecting project templates
 * for matched pairs to work on.
 */
@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectTemplateRepository projectTemplateRepository;
    private final Random random;

    @Autowired
    public ProjectService(ProjectTemplateRepository projectTemplateRepository) {
        this.projectTemplateRepository = projectTemplateRepository;
        this.random = new Random();
    }

    /**
     * Constructor for testing - allows injecting a custom Random instance.
     */
    ProjectService(ProjectTemplateRepository projectTemplateRepository, Random random) {
        this.projectTemplateRepository = projectTemplateRepository;
        this.random = random;
    }

    /**
     * Retrieves all available project templates.
     *
     * @return List of all project templates
     */
    public List<ProjectTemplate> getAllTemplates() {
        return projectTemplateRepository.findAll();
    }

    /**
     * Selects a random project template.
     * Used when assigning a project to a newly matched pair.
     *
     * @return A randomly selected ProjectTemplate
     * @throws ResourceNotFoundException if no templates exist
     */
    public ProjectTemplate getRandomTemplate() {
        List<ProjectTemplate> templates = projectTemplateRepository.findAll();
        
        if (templates.isEmpty()) {
            throw new ResourceNotFoundException("No project templates available");
        }
        
        int randomIndex = random.nextInt(templates.size());
        return templates.get(randomIndex);
    }
}
