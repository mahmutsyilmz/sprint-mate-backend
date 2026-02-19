package com.sprintmate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintmate.model.Match;
import com.sprintmate.model.MatchProject;
import com.sprintmate.model.ProjectPromptContext;
import com.sprintmate.model.SprintReview;
import com.sprintmate.repository.MatchProjectRepository;
import com.sprintmate.repository.SprintReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

/**
 * Service for generating AI-powered sprint reviews.
 *
 * Business Intent:
 * Analyzes a submitted GitHub README against the original crisis scenario
 * to provide constructive feedback, score, and identify strengths/gaps.
 */
@Service
@Slf4j
public class SprintReviewService {

    private final RestClient groqRestClient;
    private final GitHubService gitHubService;
    private final SprintReviewRepository sprintReviewRepository;
    private final MatchProjectRepository matchProjectRepository;
    private final ObjectMapper objectMapper;

    private static final String MODEL_NAME = "llama-3.3-70b-versatile";

    public SprintReviewService(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.base-url}") String baseUrl,
            GitHubService gitHubService,
            SprintReviewRepository sprintReviewRepository,
            MatchProjectRepository matchProjectRepository,
            ObjectMapper objectMapper) {

        this.groqRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.gitHubService = gitHubService;
        this.sprintReviewRepository = sprintReviewRepository;
        this.matchProjectRepository = matchProjectRepository;
        this.objectMapper = objectMapper;

        log.info("SprintReviewService initialized with model: {}", MODEL_NAME);
    }

    /**
     * Generates an AI review for a completed sprint.
     *
     * @param match  The completed match
     * @param repoUrl The GitHub repository URL submitted by the user
     * @return Optional containing the review, empty if review generation failed
     */
    @Transactional
    public Optional<SprintReview> generateReview(Match match, String repoUrl) {
        log.info("Generating sprint review for match {} with repo {}", match.getId(), repoUrl);

        try {
            // Fetch README content
            String readmeContent = gitHubService.fetchReadme(repoUrl);
            if (readmeContent == null || readmeContent.isBlank()) {
                log.warn("Empty README fetched for match {}", match.getId());
                return createEmptyReview(match, repoUrl, "No README content found");
            }

            // Get the crisis context for this match
            Optional<MatchProject> matchProjectOpt = matchProjectRepository.findByMatch(match);
            if (matchProjectOpt.isEmpty()) {
                log.warn("No MatchProject found for match {}", match.getId());
                return createEmptyReview(match, repoUrl, "No project context available");
            }

            MatchProject matchProject = matchProjectOpt.get();
            ProjectPromptContext context = matchProject.getProjectPromptContext();

            // If no crisis context, create basic review without context comparison
            if (context == null) {
                log.info("No crisis context for match {}, creating basic review", match.getId());
                return createBasicReview(match, repoUrl, readmeContent);
            }

            // Generate AI review with crisis context
            return generateAiReview(match, repoUrl, readmeContent, context);

        } catch (Exception e) {
            log.error("Failed to generate review for match {}: {}", match.getId(), e.getMessage(), e);
            return createEmptyReview(match, repoUrl, "Review generation failed: " + e.getMessage());
        }
    }

    /**
     * Generates an AI-powered review comparing README against crisis context.
     */
    private Optional<SprintReview> generateAiReview(Match match, String repoUrl,
                                                     String readmeContent, ProjectPromptContext context) {
        try {
            String prompt = buildReviewPrompt(context, readmeContent);
            String jsonResponse = callGroqApi(prompt);
            ReviewResult result = parseReviewResponse(jsonResponse);

            SprintReview review = SprintReview.builder()
                    .match(match)
                    .repoUrl(repoUrl)
                    .score(result.score())
                    .aiFeedback(result.feedback())
                    .strengths(objectMapper.writeValueAsString(result.strengths()))
                    .missingElements(objectMapper.writeValueAsString(result.missingElements()))
                    .readmeContent(truncateContent(readmeContent, 9000))
                    .build();

            SprintReview saved = sprintReviewRepository.save(review);
            log.info("Successfully generated AI review for match {} with score {}", match.getId(), result.score());
            return Optional.of(saved);

        } catch (Exception e) {
            log.error("AI review generation failed for match {}: {}", match.getId(), e.getMessage());
            return createEmptyReview(match, repoUrl, "AI analysis failed");
        }
    }

    /**
     * Creates a basic review when no crisis context is available.
     */
    private Optional<SprintReview> createBasicReview(Match match, String repoUrl, String readmeContent) {
        try {
            SprintReview review = SprintReview.builder()
                    .match(match)
                    .repoUrl(repoUrl)
                    .score(50)
                    .aiFeedback("Review completed without crisis context comparison. The README was submitted successfully.")
                    .strengths("[]")
                    .missingElements("[]")
                    .readmeContent(truncateContent(readmeContent, 9000))
                    .build();

            return Optional.of(sprintReviewRepository.save(review));
        } catch (Exception e) {
            log.error("Failed to create basic review: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Creates an empty review with score 0 when review generation fails.
     */
    private Optional<SprintReview> createEmptyReview(Match match, String repoUrl, String reason) {
        try {
            SprintReview review = SprintReview.builder()
                    .match(match)
                    .repoUrl(repoUrl)
                    .score(0)
                    .aiFeedback(reason)
                    .strengths("[]")
                    .missingElements("[]")
                    .readmeContent(null)
                    .build();

            return Optional.of(sprintReviewRepository.save(review));
        } catch (Exception e) {
            log.error("Failed to create empty review: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Builds the AI prompt for sprint review.
     */
    private String buildReviewPrompt(ProjectPromptContext context, String readmeContent) {
        return """
            You are a Senior CTO auditing a completed crisis response project.

            ═══════════════════════════════════════════════════════════════════
            ORIGINAL CRISIS CONTEXT
            ═══════════════════════════════════════════════════════════════════
            Industry: %s (%s)
            Crisis Category: %s
            Crisis Scenario: %s

            Constraints:
            - Primary: %s
            - Secondary: %s

            Compliance Requirement: %s
            Success Metric: %s
            Timeline: %s

            %s
            %s

            ═══════════════════════════════════════════════════════════════════
            SUBMITTED README
            ═══════════════════════════════════════════════════════════════════
            %s

            ═══════════════════════════════════════════════════════════════════
            YOUR TASK
            ═══════════════════════════════════════════════════════════════════
            Evaluate how well the submitted README addresses the crisis scenario.

            Consider:
            1. Does the solution address the core crisis problem?
            2. Are the specified constraints respected?
            3. Is there evidence of compliance considerations?
            4. Does the solution meet the success metrics?
            5. Is the documentation clear and professional?

            Return ONLY valid JSON (no markdown, no code blocks):
            {
              "score": <0-100 integer>,
              "feedback": "<2-3 sentence constructive feedback>",
              "strengths": ["<strength 1>", "<strength 2>", ...],
              "missing_elements": ["<missing 1>", "<missing 2>", ...]
            }
            """.formatted(
                context.getIndustry(),
                context.getSubDomain() != null ? context.getSubDomain() : "General",
                context.getCrisisCategory() != null ? context.getCrisisCategory() : "Unknown",
                context.getCrisisScenario() != null ? context.getCrisisScenario() : "No scenario provided",
                context.getPrimaryConstraint() != null ? context.getPrimaryConstraint() : "None specified",
                context.getSecondaryConstraint() != null ? context.getSecondaryConstraint() : "None specified",
                context.getComplianceRequirement() != null ? context.getComplianceRequirement() : "None specified",
                context.getSuccessMetric() != null ? context.getSuccessMetric() : "Not defined",
                context.getTimeline() != null ? context.getTimeline() : "Not specified",
                context.getLegacySystemIssue() != null ? "Legacy Issue: " + context.getLegacySystemIssue() : "",
                context.getIntegrationChallenge() != null ? "Integration Challenge: " + context.getIntegrationChallenge() : "",
                truncateContent(readmeContent, 4000)
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

            String responseBody = groqRestClient.post()
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return responseBody;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Groq request", e);
        }
    }

    private ReviewResult parseReviewResponse(String responseBody) throws JsonProcessingException {
        GroqResponse response = objectMapper.readValue(responseBody, GroqResponse.class);

        if (response.choices == null || response.choices.isEmpty()) {
            throw new RuntimeException("Groq returned no choices");
        }

        String contentJson = response.choices.get(0).message.content;
        JsonNode resultNode = objectMapper.readTree(contentJson);

        int score = resultNode.path("score").asInt(50);
        String feedback = resultNode.path("feedback").asText("No feedback provided");

        List<String> strengths = objectMapper.convertValue(
                resultNode.path("strengths"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );

        List<String> missingElements = objectMapper.convertValue(
                resultNode.path("missing_elements"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );

        // Ensure score is within bounds
        score = Math.max(0, Math.min(100, score));

        return new ReviewResult(score, feedback, strengths, missingElements);
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return null;
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "\n... [truncated]";
    }

    // Internal DTOs
    private record ReviewResult(int score, String feedback, List<String> strengths, List<String> missingElements) {}
    private record GroqRequest(String model, List<Message> messages, ResponseFormat response_format) {}
    private record Message(String role, String content) {}
    private record ResponseFormat(String type) {}
    private record GroqResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
}
