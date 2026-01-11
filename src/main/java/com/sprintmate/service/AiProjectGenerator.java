package com.sprintmate.service;

import com.sprintmate.model.ProjectTemplate;
import com.sprintmate.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI-powered implementation of ProjectGeneratorService.
 * 
 * Business Intent:
 * Uses AI models (OpenAI/Gemini) to generate personalized project templates
 * based on the combined skills of matched Frontend and Backend developers.
 * 
 * Current Status: PLACEHOLDER
 * This implementation is a stub awaiting AI integration.
 */
@Service
@Slf4j
public class AiProjectGenerator implements ProjectGeneratorService {

    // TODO: Future AI Integration - Configuration
    // 1. Add @Value annotations for API keys and model configuration:
    //    @Value("${ai.openai.api-key}") private String openAiApiKey;
    //    @Value("${ai.model}") private String modelName; // e.g., "gpt-4", "gemini-pro"
    //    @Value("${ai.temperature}") private double temperature; // 0.7 for creativity

    // TODO: Future AI Integration - Dependencies
    // 2. Inject HTTP client or AI SDK:
    //    private final RestTemplate restTemplate; // or WebClient for reactive
    //    private final ObjectMapper objectMapper; // for JSON parsing

    /**
     * Generates a personalized project template using AI.
     * 
     * TODO: Future AI Integration - Implementation Plan
     * 
     * Step 1: Extract and combine skills from both users
     * -------------------------------------------------
     * Set<String> frontendSkills = frontendUser.getSkills(); // e.g., ["React", "TypeScript", "Tailwind"]
     * Set<String> backendSkills = backendUser.getSkills();   // e.g., ["Java", "Spring Boot", "PostgreSQL"]
     * 
     * Step 2: Construct the AI prompt
     * -------------------------------
     * String prompt = """
     *     You are a senior technical project manager. Create a 1-week collaborative project 
     *     for a pair of developers with the following skills:
     *     
     *     Frontend Developer Skills: %s
     *     Backend Developer Skills: %s
     *     
     *     Requirements:
     *     - The project should be completable in 5-7 working days
     *     - Include clear separation of frontend and backend responsibilities
     *     - Use the specific technologies from each developer's skill set
     *     - The project should be portfolio-worthy and demonstrate real-world skills
     *     - Include 3-5 user stories with acceptance criteria
     *     
     *     Respond in JSON format:
     *     {
     *         "title": "Project Title",
     *         "description": "Detailed project description...",
     *         "frontendTasks": ["task1", "task2", ...],
     *         "backendTasks": ["task1", "task2", ...],
     *         "userStories": [
     *             {"story": "As a user...", "acceptance": ["criteria1", "criteria2"]}
     *         ],
     *         "techStack": {
     *             "frontend": ["React", "TypeScript"],
     *             "backend": ["Java", "Spring Boot"]
     *         }
     *     }
     *     """.formatted(frontendSkills, backendSkills);
     * 
     * Step 3: Call AI API (OpenAI example)
     * ------------------------------------
     * HttpHeaders headers = new HttpHeaders();
     * headers.setBearerAuth(openAiApiKey);
     * headers.setContentType(MediaType.APPLICATION_JSON);
     * 
     * Map<String, Object> requestBody = Map.of(
     *     "model", modelName,
     *     "messages", List.of(Map.of("role", "user", "content", prompt)),
     *     "temperature", temperature,
     *     "response_format", Map.of("type", "json_object")
     * );
     * 
     * ResponseEntity<String> response = restTemplate.exchange(
     *     "https://api.openai.com/v1/chat/completions",
     *     HttpMethod.POST,
     *     new HttpEntity<>(requestBody, headers),
     *     String.class
     * );
     * 
     * Step 4: Parse JSON response into ProjectTemplate
     * -------------------------------------------------
     * JsonNode root = objectMapper.readTree(response.getBody());
     * String content = root.path("choices").get(0).path("message").path("content").asText();
     * ProjectGenerationResponse aiResponse = objectMapper.readValue(content, ProjectGenerationResponse.class);
     * 
     * return ProjectTemplate.builder()
     *     .title(aiResponse.title())
     *     .description(aiResponse.description())
     *     .build();
     * 
     * Step 5: Error handling and fallback
     * ------------------------------------
     * - Implement retry logic with exponential backoff
     * - Fall back to random template selection if AI fails
     * - Log AI responses for quality monitoring
     * - Cache similar skill combinations to reduce API calls
     *
     * @param frontendUser The matched frontend developer with their skills
     * @param backendUser  The matched backend developer with their skills
     * @return A ProjectTemplate (currently returns null as placeholder)
     */
    @Override
    public ProjectTemplate generateProject(User frontendUser, User backendUser) {
        log.info("AI Project Generation requested for Frontend: {} (skills: {}) and Backend: {} (skills: {})",
                frontendUser.getName(), frontendUser.getSkills(),
                backendUser.getName(), backendUser.getSkills());

        // TODO: Implement AI integration as described above
        // For now, return null to indicate AI generation is not yet available
        // The MatchService should fall back to random template selection when this returns null
        
        log.warn("AI Project Generation not yet implemented - returning null for fallback to template selection");
        return null;
    }
}
