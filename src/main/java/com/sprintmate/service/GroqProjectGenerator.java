package com.sprintmate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintmate.model.ProjectPromptContext;
import com.sprintmate.model.ProjectTemplate;
import com.sprintmate.model.User;
import com.sprintmate.repository.ProjectPromptContextRepository;
import com.sprintmate.repository.ProjectTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Groq-powered implementation of ProjectGeneratorService.
 *
 * CRISIS MODE Implementation:
 * - Fetches random crisis scenarios from project_prompt_contexts table
 * - Merges crisis context with user skills for realistic project generation
 * - Key Rule: PROBLEM comes from DB, SOLUTION STACK comes from user skills
 *
 * Model: Llama-3.3-70b-versatile
 * Features:
 * - Extremely fast inference (LPU architecture)
 * - OpenAI-compatible API structure
 * - Native JSON mode support for reliable parsing
 */
@Service
@Primary
@Slf4j
public class GroqProjectGenerator implements ProjectGeneratorService {

    private final RestClient groqRestClient;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectPromptContextRepository promptContextRepository;
    private final ObjectMapper objectMapper;

    // Groq's Llama 3 model (Best balance of speed and intelligence)
    private static final String MODEL_NAME = "llama-3.3-70b-versatile";
    private static final String DEFAULT_TOPIC = "Web Application";

    public GroqProjectGenerator(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.base-url}") String baseUrl,
            ProjectTemplateRepository projectTemplateRepository,
            ProjectPromptContextRepository promptContextRepository,
            ObjectMapper objectMapper) {

        this.groqRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.projectTemplateRepository = projectTemplateRepository;
        this.promptContextRepository = promptContextRepository;
        this.objectMapper = objectMapper;

        log.info("GroqProjectGenerator initialized with model: {} (Crisis Mode enabled)", MODEL_NAME);
    }

    @Override
    public ProjectTemplate generateProject(User frontendUser, User backendUser, String topic) {
        log.info("Generating AI project via Groq (Llama 3) - Crisis Mode");

        try {
            // Try to fetch a random crisis context for Crisis Mode
            Optional<ProjectPromptContext> crisisContextOpt = fetchRandomCrisisContext();

            String systemPrompt;
            if (crisisContextOpt.isPresent()) {
                // CRISIS MODE: Use crisis scenario with user skills
                ProjectPromptContext crisis = crisisContextOpt.get();
                log.info("Crisis Mode activated - Industry: {}, Crisis: {}",
                        crisis.getIndustry(), crisis.getCrisisCategory());
                systemPrompt = buildCrisisModePrompt(crisis, frontendUser.getSkills(), backendUser.getSkills());
            } else {
                // Fallback to standard mode if no crisis contexts available
                String effectiveTopic = (topic == null || topic.isBlank()) ? DEFAULT_TOPIC : topic;
                log.info("No crisis context found, using standard mode with topic: {}", effectiveTopic);
                systemPrompt = buildStandardPrompt(frontendUser.getSkills(), backendUser.getSkills(), effectiveTopic);
            }

            String jsonResponse = callGroqApi(systemPrompt);
            ProjectTemplate template = parseGroqResponse(jsonResponse);

            // Persist the generated template
            ProjectTemplate savedTemplate = projectTemplateRepository.save(template);
            log.info("Successfully generated project: '{}'", savedTemplate.getTitle());

            return savedTemplate;
        } catch (Exception e) {
            log.error("Groq generation failed, returning fallback template. Error: {}", e.getMessage());
            return createFallbackTemplate(topic != null ? topic : DEFAULT_TOPIC);
        }
    }

    /**
     * Fetches a random crisis context from the database.
     * Uses pagination with random offset for database-agnostic random selection.
     */
    private Optional<ProjectPromptContext> fetchRandomCrisisContext() {
        log.info("=== CRISIS MODE DEBUG START ===");
        try {
            // First, get the total count
            log.info("Attempting to count project_prompt_contexts...");
            long totalCount = promptContextRepository.count();
            log.info("Total crisis contexts in DB: {}", totalCount);

            if (totalCount == 0) {
                log.warn("No crisis contexts found in database, Crisis Mode unavailable");
                return Optional.empty();
            }

            // Generate a random offset
            Random random = new Random();
            int randomOffset = random.nextInt((int) totalCount);
            log.info("Generated random offset: {} (out of {})", randomOffset, totalCount);

            // Fetch one record at the random offset
            log.info("Fetching page at offset {} with size 1...", randomOffset);
            Page<ProjectPromptContext> page = promptContextRepository.findAllPaged(
                PageRequest.of(randomOffset, 1)
            );

            log.info("Page result - hasContent: {}, totalElements: {}, totalPages: {}",
                page.hasContent(), page.getTotalElements(), page.getTotalPages());

            if (page.hasContent()) {
                ProjectPromptContext context = page.getContent().get(0);
                log.info("=== CRISIS CONTEXT FETCHED ===");
                log.info("Industry: {}", context.getIndustry());
                log.info("Sub-Domain: {}", context.getSubDomain());
                log.info("Crisis Category: {}", context.getCrisisCategory());
                log.info("Crisis Scenario: {}", context.getCrisisScenario());
                log.info("=== END CRISIS CONTEXT ===");
                return Optional.of(context);
            }

            log.warn("Page has no content despite count > 0");
            return Optional.empty();
        } catch (Exception e) {
            log.error("=== CRISIS MODE FETCH FAILED ===");
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            log.error("Full stack trace:", e);
            return Optional.empty();
        }
    }

    /**
     * Builds a CRISIS MODE prompt that combines crisis scenario with user skills.
     *
     * KEY RULE: The PROBLEM comes from the crisis context,
     * but the SOLUTION STACK comes from the user's skills - NEVER force a tech stack!
     */
    private String buildCrisisModePrompt(ProjectPromptContext crisis, Set<String> frontendSkills, Set<String> backendSkills) {
        return """
            You are a CTO acting in CRISIS MODE. A real emergency has hit the company and you need to mobilize your team IMMEDIATELY.

            ═══════════════════════════════════════════════════════════════════
            THE CRISIS CONTEXT
            ═══════════════════════════════════════════════════════════════════
            🏢 Industry: %s (%s)
            🏗️ Company Stage: %s | Team Size: %s

            🚨 CRISIS CATEGORY: %s
            📋 THE SITUATION: %s

            ⏰ Urgency Level: %s
            📊 Stakeholder Pressure: %s

            🎯 Success Metric: %s
            ⏱️ Timeline: %s
            💰 Budget: %s

            🔧 Primary Constraint: %s
            🔧 Secondary Constraint: %s

            %s
            %s
            %s

            ═══════════════════════════════════════════════════════════════════
            YOUR TEAM (Use ONLY their skills - DO NOT suggest other technologies!)
            ═══════════════════════════════════════════════════════════════════
            👨‍💻 Frontend Developer Skills: %s
            👩‍💻 Backend Developer Skills: %s

            CRITICAL RULE: You MUST design the solution using ONLY the skills listed above.
            If the legacy system used Rust but your backend dev knows Java - use Java!
            The team's existing skills are NON-NEGOTIABLE.

            ═══════════════════════════════════════════════════════════════════
            YOUR MISSION
            ═══════════════════════════════════════════════════════════════════
            Generate a CONCRETE project plan to solve this crisis using your team's skills.

            REQUIREMENTS:
            1. The project title should reflect the crisis (e.g., "Emergency Patient Portal Recovery")
            2. Tasks must be SPECIFIC and ACTIONABLE (no "Research" or "Plan" tasks)
            3. Define exact API contracts that solve the business problem
            4. Consider the constraints and compliance requirements
            5. The solution must be achievable within the timeline

            OUTPUT FORMAT (Strictly valid JSON only, no markdown):
            {
              "title": "Crisis-Driven Project Name",
              "description": "Executive summary of the crisis and technical solution approach.",
              "frontendTasks": [
                "Specific UI/UX task addressing the crisis",
                "Specific integration task",
                "Specific feature task",
                "Specific task"
              ],
              "backendTasks": [
                "Specific API/service task addressing the crisis",
                "Specific data layer task",
                "Specific security/compliance task",
                "Specific task"
              ],
              "apiEndpoints": [
                { "method": "POST", "path": "/api/v1/endpoint", "description": "Purpose related to crisis" },
                { "method": "GET", "path": "/api/v1/endpoint", "description": "Purpose related to crisis" }
              ]
            }
            """.formatted(
                crisis.getIndustry(),
                crisis.getSubDomain() != null ? crisis.getSubDomain() : "General",
                crisis.getCompanyStage() != null ? crisis.getCompanyStage() : "Unknown",
                crisis.getTeamSize() != null ? crisis.getTeamSize() : "Small",
                crisis.getCrisisCategory() != null ? crisis.getCrisisCategory() : "System Failure",
                crisis.getCrisisScenario() != null ? crisis.getCrisisScenario() : "Critical system requires immediate replacement",
                crisis.getUrgencyLevel() != null ? crisis.getUrgencyLevel() : "High",
                crisis.getStakeholderPressure() != null ? crisis.getStakeholderPressure() : "Management expecting quick resolution",
                crisis.getSuccessMetric() != null ? crisis.getSuccessMetric() : "System operational",
                crisis.getTimeline() != null ? crisis.getTimeline() : "1 week",
                crisis.getBudgetConstraint() != null ? crisis.getBudgetConstraint() : "Limited",
                crisis.getPrimaryConstraint() != null ? crisis.getPrimaryConstraint() : "Time",
                crisis.getSecondaryConstraint() != null ? crisis.getSecondaryConstraint() : "Resources",
                crisis.getLegacySystemIssue() != null ? "🏚️ Legacy Issue: " + crisis.getLegacySystemIssue() : "",
                crisis.getComplianceRequirement() != null ? "📜 Compliance: " + crisis.getComplianceRequirement() : "",
                crisis.getIntegrationChallenge() != null ? "🔗 Integration Challenge: " + crisis.getIntegrationChallenge() : "",
                frontendSkills,
                backendSkills
        );
    }

    /**
     * Builds a standard prompt for when no crisis context is available.
     * This is the fallback mode.
     */
    private String buildStandardPrompt(Set<String> frontendSkills, Set<String> backendSkills, String topic) {
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