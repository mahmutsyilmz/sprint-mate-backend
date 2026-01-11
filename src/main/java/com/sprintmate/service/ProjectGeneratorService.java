package com.sprintmate.service;

import com.sprintmate.model.ProjectTemplate;
import com.sprintmate.model.User;

/**
 * Service interface for AI-driven project generation.
 * 
 * Business Intent:
 * Defines the contract for generating personalized project templates
 * based on the combined skills of matched developers (Frontend + Backend).
 * 
 * Future implementations will integrate with AI models (OpenAI, Gemini, etc.)
 * to create unique, skill-appropriate project suggestions for each match.
 */
public interface ProjectGeneratorService {

    /**
     * Generates a personalized project template based on the skills of two matched users.
     * 
     * The generated project should:
     * - Leverage the combined tech stack of both developers
     * - Be completable within a 1-week sprint
     * - Include clear frontend and backend responsibilities
     * - Provide learning opportunities for both participants
     *
     * @param frontendUser The user with FRONTEND role and their associated skills
     * @param backendUser  The user with BACKEND role and their associated skills
     * @return A ProjectTemplate tailored to the combined skill sets
     */
    ProjectTemplate generateProject(User frontendUser, User backendUser);
}
