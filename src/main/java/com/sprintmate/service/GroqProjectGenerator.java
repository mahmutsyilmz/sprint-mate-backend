package com.sprintmate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintmate.model.ProjectIdea;
import com.sprintmate.model.ProjectTemplate;
import com.sprintmate.model.User;
import com.sprintmate.repository.ProjectIdeaRepository;
import com.sprintmate.repository.ProjectTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Groq-powered implementation of ProjectGeneratorService.
 *
 * NEW APPROACH: Fun, Portfolio-Worthy Projects
 * Instead of stressful "crisis scenarios", we generate exciting mini-projects
 * that developers would actually want to build and show off.
 *
 * Model: Llama-3.3-70b-versatile
 */
@Service
@Primary
@Slf4j
public class GroqProjectGenerator implements ProjectGeneratorService {

    private final RestClient groqRestClient;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectIdeaRepository projectIdeaRepository;
    private final ObjectMapper objectMapper;

    private static final String MODEL_NAME = "llama-3.3-70b-versatile";

    public GroqProjectGenerator(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.base-url}") String baseUrl,
            RestClient.Builder restClientBuilder,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectIdeaRepository projectIdeaRepository,
            ObjectMapper objectMapper) {

        this.groqRestClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.projectTemplateRepository = projectTemplateRepository;
        this.projectIdeaRepository = projectIdeaRepository;
        this.objectMapper = objectMapper;

        log.info("GroqProjectGenerator initialized with model: {}", MODEL_NAME);
    }

    @Override
    public GeneratedProject generateProject(User frontendUser, User backendUser, String topic) {
        log.info("Generating fun project for {} (FE) and {} (BE)",
                frontendUser.getName(), backendUser.getName());

        try {
            // Try to fetch a random project idea
            Optional<ProjectIdea> ideaOpt = fetchRandomProjectIdea();
            ProjectIdea usedIdea = ideaOpt.orElse(null);

            String systemPrompt = buildPrompt(
                    frontendUser.getSkills(),
                    backendUser.getSkills(),
                    usedIdea
            );

            String jsonResponse = callGroqApi(systemPrompt);
            ProjectTemplate template = parseGroqResponse(jsonResponse);

            ProjectTemplate savedTemplate = projectTemplateRepository.save(template);
            log.info("Generated project: '{}'", savedTemplate.getTitle());

            return new GeneratedProject(savedTemplate, usedIdea);
        } catch (Exception e) {
            log.error("Project generation failed: {}", e.getMessage());
            return new GeneratedProject(createFallbackTemplate(), null);
        }
    }

    /**
     * Fetches a random project idea from the database.
     */
    private Optional<ProjectIdea> fetchRandomProjectIdea() {
        try {
            long totalCount = projectIdeaRepository.countActive();
            if (totalCount == 0) {
                log.info("No project ideas in database, using AI creativity");
                return Optional.empty();
            }

            Random random = new Random();
            int randomOffset = random.nextInt((int) totalCount);
            Page<ProjectIdea> page = projectIdeaRepository.findAllActivePaged(
                    PageRequest.of(randomOffset, 1)
            );

            if (page.hasContent()) {
                ProjectIdea idea = page.getContent().get(0);
                log.info("Selected project idea: {} ({})", idea.getName(), idea.getCategory());
                return Optional.of(idea);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to fetch project idea: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Builds an exciting prompt that generates fun, portfolio-worthy projects.
     */
    private String buildPrompt(Set<String> frontendSkills, Set<String> backendSkills, ProjectIdea idea) {
        String ideaSection = "";
        if (idea != null) {
            ideaSection = """

                PROJECT INSPIRATION (Use this as a starting point, but make it unique!):
                Category: %s
                Concept: %s
                Pitch: %s
                Key Features to Include: %s
                Example Use Case: %s

                """.formatted(
                    idea.getCategory(),
                    idea.getCoreConcept(),
                    idea.getPitch(),
                    idea.getKeyFeatures(),
                    idea.getExampleUseCase() != null ? idea.getExampleUseCase() : "Be creative!"
            );
        }

        return """
            You are a creative tech mentor helping two developers build something AWESOME together.
            Your job is to design a fun, achievable mini-project they can complete in 1 WEEK.

            THE TEAM:
            Frontend Developer knows: %s
            Backend Developer knows: %s
            %s
            DESIGN PRINCIPLES:
            1. FUN FIRST - The project should be something developers are EXCITED to build
            2. PORTFOLIO-WORTHY - Something they'd proudly show to recruiters
            3. ACHIEVABLE - Must be completable in 1 week by 2 people
            4. MODERN - Use their actual skills, no legacy technologies
            5. COLLABORATIVE - Clear separation between frontend and backend work
            6. REAL VALUE - Something that could actually be used by real people

            PROJECT REQUIREMENTS:
            - 4-6 specific frontend tasks (UI components, state management, API integration)
            - 4-6 specific backend tasks (APIs, database, business logic)
            - 4-8 API endpoints with clear purposes
            - The project should have a clear "wow factor" - something that impresses

            GOOD PROJECT EXAMPLES:
            - A real-time collaborative todo app with live updates
            - A mini social platform with posts, likes, and comments
            - A personal finance tracker with charts and insights
            - A recipe sharing app with search and favorites
            - A quiz/trivia game with leaderboards
            - A bookmark manager with tags and search

            BAD PROJECT EXAMPLES (AVOID THESE):
            - Generic CRUD apps with no personality
            - Enterprise systems or "crisis recovery" projects
            - Anything requiring legacy tech (Classic ASP, COBOL, etc.)
            - Projects that are too complex for 1 week
            - Boring "database refactoring" or "connection pool optimization"

            OUTPUT FORMAT (Strictly valid JSON, no markdown):
            {
              "title": "Catchy Project Name (e.g., 'SnapShare - Mini Photo Platform')",
              "description": "2-3 sentence pitch that makes developers excited to build this",
              "wowFactor": "What makes this project impressive (1 sentence)",
              "frontendTasks": [
                "Specific task with technology (e.g., 'Build responsive feed UI with infinite scroll using React')",
                "Another specific task",
                "..."
              ],
              "backendTasks": [
                "Specific task with technology (e.g., 'Implement JWT auth with refresh tokens in Spring Boot')",
                "Another specific task",
                "..."
              ],
              "apiEndpoints": [
                { "method": "POST", "path": "/api/auth/login", "description": "User login, returns JWT" },
                { "method": "GET", "path": "/api/posts", "description": "Get paginated feed" },
                "..."
              ]
            }
            """.formatted(
                frontendSkills.isEmpty() ? "React, TypeScript, CSS" : frontendSkills,
                backendSkills.isEmpty() ? "Java, Spring Boot, PostgreSQL" : backendSkills,
                ideaSection
        );
    }

    @Retryable(
            retryFor = {HttpClientErrorException.TooManyRequests.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    protected String callGroqApi(String systemPrompt) {
        GroqRequest request = new GroqRequest(
                MODEL_NAME,
                List.of(new Message("system", systemPrompt)),
                new ResponseFormat("json_object")
        );

        try {
            String requestBody = objectMapper.writeValueAsString(request);

            return groqRestClient.post()
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Groq request", e);
        }
    }

    private ProjectTemplate parseGroqResponse(String responseBody) throws JsonProcessingException {
        GroqResponse response = objectMapper.readValue(responseBody, GroqResponse.class);

        if (response.choices == null || response.choices.isEmpty()) {
            throw new RuntimeException("Groq returned no choices");
        }

        String contentJson = response.choices.get(0).message.content;
        JsonNode projectNode = objectMapper.readTree(contentJson);

        return ProjectTemplate.builder()
                .title(projectNode.path("title").asText())
                .description(buildDescription(projectNode))
                .build();
    }

    private String buildDescription(JsonNode projectJson) {
        StringBuilder sb = new StringBuilder();

        // Project pitch
        sb.append(projectJson.path("description").asText()).append("\n\n");

        // Wow factor
        String wowFactor = projectJson.path("wowFactor").asText();
        if (!wowFactor.isBlank()) {
            sb.append("✨ What makes this special: ").append(wowFactor).append("\n\n");
        }

        // API Contract
        JsonNode apiEndpoints = projectJson.path("apiEndpoints");
        if (apiEndpoints.isArray() && !apiEndpoints.isEmpty()) {
            sb.append("📡 API Contract:\n");
            for (JsonNode api : apiEndpoints) {
                String method = api.path("method").asText();
                String path = api.path("path").asText();
                String desc = api.path("description").asText();
                sb.append(String.format("• [%s] %s - %s\n", method, path, desc));
            }
            sb.append("\n");
        }

        // Frontend Tasks
        JsonNode frontendTasks = projectJson.path("frontendTasks");
        if (frontendTasks.isArray() && !frontendTasks.isEmpty()) {
            sb.append("🎨 Frontend Tasks:\n");
            frontendTasks.forEach(t -> sb.append("• ").append(t.asText()).append("\n"));
            sb.append("\n");
        }

        // Backend Tasks
        JsonNode backendTasks = projectJson.path("backendTasks");
        if (backendTasks.isArray() && !backendTasks.isEmpty()) {
            sb.append("⚙️ Backend Tasks:\n");
            backendTasks.forEach(t -> sb.append("• ").append(t.asText()).append("\n"));
        }

        return sb.toString();
    }

    private ProjectTemplate createFallbackTemplate() {
        return projectTemplateRepository.save(ProjectTemplate.builder()
                .title("Collaborative Mini Project")
                .description("""
                        Build a simple but impressive web application together!

                        ✨ What makes this special: You get to decide the direction!

                        📡 Suggested API Contract:
                        • [POST] /api/auth/register - User registration
                        • [POST] /api/auth/login - User login with JWT
                        • [GET] /api/items - List items with pagination
                        • [POST] /api/items - Create new item
                        • [GET] /api/items/{id} - Get item details

                        🎨 Frontend Tasks:
                        • Build login/register forms with validation
                        • Create main dashboard with item listing
                        • Implement create/edit item modal
                        • Add responsive navigation

                        ⚙️ Backend Tasks:
                        • Set up JWT authentication
                        • Create database schema for items
                        • Implement CRUD REST APIs
                        • Add pagination and filtering
                        """)
                .build());
    }

    // DTOs for Groq API
    private record GroqRequest(String model, List<Message> messages, ResponseFormat response_format) {}
    private record Message(String role, String content) {}
    private record ResponseFormat(String type) {}
    private record GroqResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
}
