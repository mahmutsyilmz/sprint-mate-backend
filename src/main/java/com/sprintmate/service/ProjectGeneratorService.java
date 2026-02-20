package com.sprintmate.service;

import com.sprintmate.model.ProjectArchetype;
import com.sprintmate.model.ProjectIdea;
import com.sprintmate.model.ProjectTemplate;
import com.sprintmate.model.ProjectTheme;
import com.sprintmate.model.User;

/**
 * Service interface for AI-driven project generation.
 *
 * Business Intent:
 * Defines the contract for generating personalized project templates
 * based on the combined skills of matched developers (Frontend + Backend),
 * their preferences (archetype, theme, difficulty, learning goals),
 * and an optional project topic.
 */
public interface ProjectGeneratorService {

    /**
     * Result record containing the generated template and selection metadata.
     *
     * @param template  The generated project template
     * @param idea      The legacy project idea (deprecated, may be null)
     * @param archetype The archetype used for generation (may be null for legacy)
     * @param theme     The theme used for generation (may be null for legacy)
     */
    record GeneratedProject(
            ProjectTemplate template,
            @Deprecated ProjectIdea idea,
            ProjectArchetype archetype,
            ProjectTheme theme
    ) {}

    /**
     * Generates a personalized project template based on the skills of two matched users
     * and an optional topic preference.
     *
     * @param frontendUser The user with FRONTEND role and their associated skills
     * @param backendUser  The user with BACKEND role and their associated skills
     * @param topic        Optional topic for the project (e.g., "Fintech", "Sports", "AI")
     * @return A GeneratedProject containing the template and selection metadata
     */
    GeneratedProject generateProject(User frontendUser, User backendUser, String topic);
}
