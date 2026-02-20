package com.sprintmate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintmate.model.ProjectTemplate;
import com.sprintmate.model.User;
import com.sprintmate.repository.ProjectTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Groq-powered implementation of ProjectGeneratorService.
 *
 * Uses archetype + theme + user preferences for personalized project generation.
 * Model: Llama-3.3-70b-versatile
 */
@Service
@Primary
@Slf4j
public class GroqProjectGenerator implements ProjectGeneratorService {

    private final RestClient groqRestClient;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectSelectionService projectSelectionService;
    private final ModularPromptBuilder modularPromptBuilder;
    private final ObjectMapper objectMapper;

    private static final String MODEL_NAME = "llama-3.3-70b-versatile";

    public GroqProjectGenerator(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.base-url}") String baseUrl,
            RestClient.Builder restClientBuilder,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectSelectionService projectSelectionService,
            ModularPromptBuilder modularPromptBuilder,
            ObjectMapper objectMapper) {

        this.groqRestClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.projectTemplateRepository = projectTemplateRepository;
        this.projectSelectionService = projectSelectionService;
        this.modularPromptBuilder = modularPromptBuilder;
        this.objectMapper = objectMapper;

        log.info("GroqProjectGenerator initialized with model: {}", MODEL_NAME);
    }

    @Override
    public GeneratedProject generateProject(User frontendUser, User backendUser, String topic) {
        log.info("Generating project for {} (FE) and {} (BE)",
                frontendUser.getName(), backendUser.getName());

        try {
            // Select archetype + theme based on user preferences
            ProjectSelectionService.SelectionResult selection =
                    projectSelectionService.select(frontendUser, backendUser);

            // Build modular prompt
            String systemPrompt = modularPromptBuilder.buildPrompt(
                    frontendUser.getSkills(),
                    backendUser.getSkills(),
                    selection.archetype(),
                    selection.theme(),
                    selection.targetComplexity(),
                    selection.frontendLearningGoals(),
                    selection.backendLearningGoals()
            );

            String jsonResponse = callGroqApi(systemPrompt);
            ProjectTemplate template = parseGroqResponse(jsonResponse);

            ProjectTemplate savedTemplate = projectTemplateRepository.save(template);
            log.info("Generated project: '{}' (archetype={}, theme={})",
                    savedTemplate.getTitle(), selection.archetype().getCode(), selection.theme().getCode());

            return new GeneratedProject(savedTemplate, null, selection.archetype(), selection.theme());
        } catch (Exception e) {
            log.error("Project generation failed: {}", e.getMessage());
            return new GeneratedProject(createFallbackTemplate(), null, null, null);
        }
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

        sb.append(projectJson.path("description").asText()).append("\n\n");

        String wowFactor = projectJson.path("wowFactor").asText();
        if (!wowFactor.isBlank()) {
            sb.append("✨ What makes this special: ").append(wowFactor).append("\n\n");
        }

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

        JsonNode frontendTasks = projectJson.path("frontendTasks");
        if (frontendTasks.isArray() && !frontendTasks.isEmpty()) {
            sb.append("🎨 Frontend Tasks:\n");
            frontendTasks.forEach(t -> sb.append("• ").append(t.asText()).append("\n"));
            sb.append("\n");
        }

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
