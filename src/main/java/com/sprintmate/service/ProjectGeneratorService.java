package com.sprintmate.service;

import com.sprintmate.model.ProjectIdea;
import com.sprintmate.model.ProjectTemplate;
import com.sprintmate.model.User;

/**
 * Service interface for AI-driven project generation.
 *
 * Business Intent:
 * Defines the contract for generating personalized project templates
 * based on the combined skills of matched developers (Frontend + Backend)
 * and an optional project topic (e.g., Fintech, Sports, AI).
 *
 * Implementations integrate with AI models (Gemini, OpenAI, etc.)
 * to create unique, skill-appropriate project suggestions for each match.
 */
public interface ProjectGeneratorService {

    /**
     * Result record containing the generated template and project idea.
     * The idea is preserved to enable AI-powered sprint reviews.
     *
     * @param template The generated project template
     * @param idea     The project idea used for generation (may be null)
     */
    record GeneratedProject(ProjectTemplate template, ProjectIdea idea) {}

    /**
     * Generates a personalized project template based on the skills of two matched users
     * and an optional topic preference.
     *
     * The generated project should:
     * - Leverage the combined tech stack of both developers
     * - Be completable within a 1-week sprint
     * - Include clear frontend and backend responsibilities
     * - Provide learning opportunities for both participants
     * - Align with the specified topic if provided
     *
     * @param frontendUser The user with FRONTEND role and their associated skills
     * @param backendUser  The user with BACKEND role and their associated skills
     * @param topic        Optional topic for the project (e.g., "Fintech", "Sports", "AI")
     * @return A GeneratedProject containing the template and crisis context
     */
    GeneratedProject generateProject(User frontendUser, User backendUser, String topic);
}
