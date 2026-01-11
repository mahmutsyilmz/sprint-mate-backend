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
import java.util.Set;

/**
 * Groq-powered implementation of ProjectGeneratorService.
 * * Model: Llama-3-70b-8192
 * Features:
 * - Extremely fast inference (LPU architecture)
 * - OpenAI-compatible API structure
 * - Native JSON mode support for reliable parsing
 * - Free tier (during Beta)
 */
@Service
@Primary
@Slf4j
public class GroqProjectGenerator implements ProjectGeneratorService {

    private final RestClient groqRestClient;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ObjectMapper objectMapper;

    // Groq's Llama 3 model (Best balance of speed and intelligence)
    private static final String MODEL_NAME = "llama-3.3-70b-versatile";
    private static final String DEFAULT_TOPIC = "Web Application";

    public GroqProjectGenerator(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.base-url}") String baseUrl,
            ProjectTemplateRepository projectTemplateRepository,
            ObjectMapper objectMapper) {
        
        this.groqRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.projectTemplateRepository = projectTemplateRepository;
        this.objectMapper = objectMapper;
        
        log.info("GroqProjectGenerator initialized with model: {}", MODEL_NAME);
    }

    @Override
    public ProjectTemplate generateProject(User frontendUser, User backendUser, String topic) {
        String effectiveTopic = (topic == null || topic.isBlank()) ? DEFAULT_TOPIC : topic;

        log.info("Generating AI project via Groq (Llama 3) for Topic: {}", effectiveTopic);

        try {
            String systemPrompt = buildSystemPrompt(frontendUser.getSkills(), backendUser.getSkills(), effectiveTopic);
            String jsonResponse = callGroqApi(systemPrompt);
            ProjectTemplate template = parseGroqResponse(jsonResponse);
            
            // Persist the generated template
            ProjectTemplate savedTemplate = projectTemplateRepository.save(template);
            log.info("Successfully generated project: '{}'", savedTemplate.getTitle());
            
            return savedTemplate;
        } catch (Exception e) {
            log.error("Groq generation failed, returning fallback template. Error: {}", e.getMessage());
            return createFallbackTemplate(effectiveTopic);
        }
    }

    private String buildSystemPrompt(Set<String> frontendSkills, Set<String> backendSkills, String topic) {
        return """
            You are a strict Senior Technical Architect assigning a high-level sprint to a Frontend and Backend pair.
            
            CONTEXT:
            - Project Topic: %s
            - Frontend Stack: %s
            - Backend Stack: %s
            
            REQUIREMENTS:
            1. DO NOT include generic tasks like "Research" or "Plan".
            2. DEFINE specific technical tasks (e.g., "Create 'users' table with UUID", "Implement JWT Filter").
            3. DEFINE the exact API Contract (Endpoints, Methods).
            4. The project must be complex enough for 1 week (e.g., include Pagination, Sorting, or File Upload).
            
            OUTPUT FORMAT:
            Strictly valid JSON only. No markdown. Structure:
            {
              "title": "Professional Project Name",
              "description": "Technical summary of the architecture and goals.",
              "frontendTasks": [
                "Detailed Task 1 (e.g., Integrate POST /api/login with Axios)",
                "Detailed Task 2",
                "Detailed Task 3",
                "Detailed Task 4"
              ],
              "backendTasks": [
                "Detailed Task 1 (e.g., Implement POST /api/orders with Transaction management)",
                "Detailed Task 2",
                "Detailed Task 3",
                "Detailed Task 4"
              ],
              "apiEndpoints": [
                { "method": "POST", "path": "/api/v1/resource", "description": "Short purpose" },
                { "method": "GET", "path": "/api/v1/resource/{id}", "description": "Short purpose" },
                { "method": "GET", "path": "/api/v1/resource", "description": "Pagination & Filtering" }
              ]
            }
            """.formatted(topic, frontendSkills, backendSkills);
    }

    @Retryable(
        retryFor = { HttpClientErrorException.TooManyRequests.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    protected String callGroqApi(String systemPrompt) {
        // Prepare Request DTO
        GroqRequest request = new GroqRequest(
            MODEL_NAME,
            List.of(new Message("system", systemPrompt)),
            new ResponseFormat("json_object") // Enforce JSON mode
        );

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            
            // Execute Call
            String responseBody = groqRestClient.post()
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            
            return responseBody;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Groq request", e);
        }
    }

    private ProjectTemplate parseGroqResponse(String responseBody) throws JsonProcessingException {
        // Parse the outer Groq response
        GroqResponse response = objectMapper.readValue(responseBody, GroqResponse.class);
        
        if (response.choices == null || response.choices.isEmpty()) {
            throw new RuntimeException("Groq returned no choices");
        }

        // Get the content string (which is the JSON project plan)
        String contentJson = response.choices.get(0).message.content;

        // Parse the inner Project JSON
        JsonNode projectNode = objectMapper.readTree(contentJson);

        return ProjectTemplate.builder()
                .title(projectNode.path("title").asText())
                .description(buildDescription(projectNode))
                .build();
    }

    private String buildDescription(JsonNode projectJson) {
        StringBuilder sb = new StringBuilder();

        // 1. Technical Summary
        sb.append(projectJson.path("description").asText()).append("\n\n");

        // 2. API Contract (The most important part)
        JsonNode apiEndpoints = projectJson.path("apiEndpoints");
        if (apiEndpoints.isArray() && !apiEndpoints.isEmpty()) {
            sb.append("📡 API Contract (Core Endpoints):\n");
            for (JsonNode api : apiEndpoints) {
                String method = api.path("method").asText();
                String path = api.path("path").asText();
                String desc = api.path("description").asText();
                // Format: [POST] /api/v1/login - Returns JWT Token
                sb.append(String.format("• [%s] %s - %s\n", method, path, desc));
            }
            sb.append("\n");
        }

        // 3. Frontend Specifics
        JsonNode frontendTasks = projectJson.path("frontendTasks");
        if (frontendTasks.isArray() && !frontendTasks.isEmpty()) {
            sb.append("🎨 Frontend Tasks:\n");
            frontendTasks.forEach(t -> sb.append("• ").append(t.asText()).append("\n"));
            sb.append("\n");
        }

        // 4. Backend Specifics
        JsonNode backendTasks = projectJson.path("backendTasks");
        if (backendTasks.isArray() && !backendTasks.isEmpty()) {
            sb.append("⚙️ Backend Tasks:\n");
            backendTasks.forEach(t -> sb.append("• ").append(t.asText()).append("\n"));
        }

        return sb.toString();
    }

    private ProjectTemplate createFallbackTemplate(String topic) {
        return projectTemplateRepository.save(ProjectTemplate.builder()
                .title("Collaborative " + topic + " Project (Fallback)")
                .description("AI Generation failed. Please proceed with a standard " + topic + " MVP structure.\n" +
                        "\nFrontend: Setup UI/UX and API integration.\nBackend: Setup Database and REST API.")
                .build());
    }

    // =================================================================
    // DTOs for OpenAI-Compatible API (Groq uses this format)
    // =================================================================

    private record GroqRequest(String model, List<Message> messages, ResponseFormat response_format) {}
    private record Message(String role, String content) {}
    private record ResponseFormat(String type) {}

    private record GroqResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
}