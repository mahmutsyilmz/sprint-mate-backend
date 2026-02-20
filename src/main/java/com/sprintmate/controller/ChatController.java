package com.sprintmate.controller;

import com.sprintmate.constant.GitHubConstants;
import com.sprintmate.dto.ChatMessageRequest;
import com.sprintmate.dto.ChatMessageResponse;
import com.sprintmate.model.User;
import com.sprintmate.repository.UserRepository;
import com.sprintmate.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Controller for chat functionality.
 * Handles both WebSocket messages and REST endpoints for chat history.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "Chat message endpoints")
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Validator validator;

    /**
     * WebSocket endpoint for sending chat messages.
     * Messages are validated manually since @Valid is not auto-enforced on @MessageMapping.
     * Messages are saved to DB and broadcast to all subscribers of the match topic.
     *
     * @param request   The chat message request
     * @param principal The authenticated user
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        // Manual validation - @Valid is not enforced on WebSocket @MessageMapping
        Set<ConstraintViolation<ChatMessageRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            log.warn("Invalid chat message rejected: {}", violations.iterator().next().getMessage());
            return;
        }

        UUID senderId = getUserIdFromPrincipal(principal);
        if (senderId == null) {
            log.warn("Could not resolve sender ID from principal");
            return;
        }

        // Save and get the response
        ChatMessageResponse response = chatService.saveMessage(request, senderId);

        // Broadcast to all subscribers of this match's topic
        String destination = "/topic/match/" + request.matchId();
        messagingTemplate.convertAndSend(destination, response);

        log.debug("Broadcast message {} to {}", response.id(), destination);
    }

    /**
     * REST endpoint for retrieving chat history.
     * Returns the most recent messages for a match conversation.
     *
     * @param matchId    The match ID to retrieve history for
     * @param limit      Optional limit for number of messages (default 100)
     * @param oauth2User The authenticated user
     * @return List of chat messages ordered chronologically
     */
    @GetMapping("/api/chat/history/{matchId}")
    @Operation(
        summary = "Get chat history for a match",
        description = "Retrieves the chat message history for a match conversation. " +
                      "Only participants of the match can access the history."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Chat history retrieved successfully",
            content = @Content(schema = @Schema(implementation = ChatMessageResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "User is not a participant in the match",
            content = @Content
        )
    })
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @PathVariable UUID matchId,
            @RequestParam(required = false, defaultValue = "100") int limit,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        UUID userId = getUserIdFromOAuth2User(oauth2User);
        List<ChatMessageResponse> history = chatService.getChatHistory(matchId, userId, limit);

        return ResponseEntity.ok(history);
    }

    /**
     * Resolves user ID from WebSocket Principal.
     */
    private UUID getUserIdFromPrincipal(Principal principal) {
        if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken authToken) {
            OAuth2User oauth2User = authToken.getPrincipal();
            return getUserIdFromOAuth2User(oauth2User);
        }
        return null;
    }

    /**
     * Resolves user ID from OAuth2User.
     */
    private UUID getUserIdFromOAuth2User(OAuth2User oauth2User) {
        String login = oauth2User.getAttribute("login");
        if (login != null) {
            String githubUrl = GitHubConstants.GITHUB_BASE_URL + login;
            return userRepository.findByGithubUrl(githubUrl)
                .map(User::getId)
                .orElse(null);
        }
        return null;
    }
}
