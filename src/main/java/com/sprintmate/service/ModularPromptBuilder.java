package com.sprintmate.service;

import com.sprintmate.model.ProjectArchetype;
import com.sprintmate.model.ProjectTheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Builds AI prompts from modular sections based on archetype, theme, skills, and preferences.
 *
 * Business Intent:
 * Replaces the monolithic prompt builder with composable sections.
 * Each section focuses on one aspect (role, team, archetype, theme, complexity, etc.)
 * enabling infinite project variety through different combinations.
 */
@Component
@Slf4j
public class ModularPromptBuilder {

    /**
     * Builds the complete AI prompt from modular sections.
     */
    public String buildPrompt(
            Set<String> frontendSkills,
            Set<String> backendSkills,
            ProjectArchetype archetype,
            ProjectTheme theme,
            int targetComplexity,
            String frontendLearningGoals,
            String backendLearningGoals
    ) {
        log.debug("Building prompt: archetype={}, theme={}, complexity={}",
                archetype.getCode(), theme.getCode(), targetComplexity);

        return String.join("\n\n",
                buildRoleSection(),
                buildTeamSection(frontendSkills, backendSkills),
                buildArchetypeSection(archetype),
                buildThemeSection(theme),
                buildComplexitySection(targetComplexity),
                buildLearningGoalsSection(frontendLearningGoals, backendLearningGoals),
                buildConstraintsSection(),
                buildOutputFormatSection()
        );
    }

    private String buildRoleSection() {
        return """
                You are a creative tech mentor helping two developers build something AWESOME together.
                Your job is to design a fun, achievable mini-project they can complete in 1 WEEK.
                The project should be portfolio-worthy and something developers are excited to build.""";
    }

    private String buildTeamSection(Set<String> frontendSkills, Set<String> backendSkills) {
        String feSkills = frontendSkills.isEmpty() ? "React, TypeScript, CSS" : String.join(", ", frontendSkills);
        String beSkills = backendSkills.isEmpty() ? "Java, Spring Boot, PostgreSQL" : String.join(", ", backendSkills);

        return """
                THE TEAM:
                Frontend Developer knows: %s
                Backend Developer knows: %s""".formatted(feSkills, beSkills);
    }

    private String buildArchetypeSection(ProjectArchetype archetype) {
        return """
                PROJECT STRUCTURE (follow this pattern):
                Type: %s
                Description: %s
                Component Patterns to use: %s
                API Patterns to use: %s

                Design the project around this structural pattern. The architecture should naturally \
                follow the patterns listed above.""".formatted(
                archetype.getDisplayName(),
                archetype.getStructureDescription(),
                archetype.getComponentPatterns() != null ? archetype.getComponentPatterns() : "REST,CRUD",
                archetype.getApiPatterns() != null ? archetype.getApiPatterns() : "REST"
        );
    }

    private String buildThemeSection(ProjectTheme theme) {
        return """
                PROJECT DOMAIN/THEME:
                Theme: %s
                Context: %s
                Example domain entities: %s

                The project must be set in this domain. Use domain-specific terminology, \
                entities, and real-world scenarios from this field.""".formatted(
                theme.getDisplayName(),
                theme.getDomainContext() != null ? theme.getDomainContext() : theme.getDisplayName(),
                theme.getExampleEntities() != null ? theme.getExampleEntities() : "item,category,user"
        );
    }

    private String buildComplexitySection(int targetComplexity) {
        String level;
        String taskGuidance;

        switch (targetComplexity) {
            case 1 -> {
                level = "Beginner-Friendly";
                taskGuidance = "3-4 frontend tasks, 3-4 backend tasks, 3-5 API endpoints. Keep it simple and achievable.";
            }
            case 3 -> {
                level = "Advanced";
                taskGuidance = "5-7 frontend tasks, 5-7 backend tasks, 6-10 API endpoints. Include challenging patterns like real-time features, complex queries, or advanced state management.";
            }
            default -> {
                level = "Intermediate";
                taskGuidance = "4-6 frontend tasks, 4-6 backend tasks, 4-8 API endpoints. Balance between learning and achievability.";
            }
        }

        return """
                COMPLEXITY LEVEL: %s
                %s""".formatted(level, taskGuidance);
    }

    private String buildLearningGoalsSection(String frontendGoals, String backendGoals) {
        if ((frontendGoals == null || frontendGoals.isBlank()) &&
            (backendGoals == null || backendGoals.isBlank())) {
            return "";
        }

        StringBuilder sb = new StringBuilder("LEARNING GOALS (incorporate where natural):\n");
        if (frontendGoals != null && !frontendGoals.isBlank()) {
            sb.append("Frontend developer wants to learn: ").append(frontendGoals).append("\n");
        }
        if (backendGoals != null && !backendGoals.isBlank()) {
            sb.append("Backend developer wants to learn: ").append(backendGoals).append("\n");
        }
        sb.append("Try to include tasks that let them practice these technologies/patterns.");
        return sb.toString();
    }

    private String buildConstraintsSection() {
        return """
                DESIGN PRINCIPLES:
                1. FUN FIRST - The project should be something developers are EXCITED to build
                2. PORTFOLIO-WORTHY - Something they'd proudly show to recruiters
                3. ACHIEVABLE - Must be completable in 1 week by 2 people
                4. MODERN - Use their actual skills, no legacy technologies
                5. COLLABORATIVE - Clear separation between frontend and backend work
                6. REAL VALUE - Something that could actually be used by real people""";
    }

    private String buildOutputFormatSection() {
        return """
                OUTPUT FORMAT (Strictly valid JSON, no markdown):
                {
                  "title": "Catchy Project Name (e.g., 'SnapShare - Mini Photo Platform')",
                  "description": "2-3 sentence pitch that makes developers excited to build this",
                  "wowFactor": "What makes this project impressive (1 sentence)",
                  "frontendTasks": [
                    "Specific task with technology (e.g., 'Build responsive feed UI with infinite scroll using React')",
                    "..."
                  ],
                  "backendTasks": [
                    "Specific task with technology (e.g., 'Implement JWT auth with refresh tokens in Spring Boot')",
                    "..."
                  ],
                  "apiEndpoints": [
                    { "method": "POST", "path": "/api/auth/login", "description": "User login, returns JWT" },
                    "..."
                  ]
                }""";
    }
}
