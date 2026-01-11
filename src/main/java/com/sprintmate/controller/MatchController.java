package com.sprintmate.controller;

import com.sprintmate.constant.GitHubConstants;
import com.sprintmate.dto.MatchCompletionRequest;
import com.sprintmate.dto.MatchCompletionResponse;
import com.sprintmate.dto.MatchStatusResponse;
import com.sprintmate.dto.UserResponse;
import com.sprintmate.service.MatchService;
import com.sprintmate.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Match-related operations.
 * 
 * Business Intent:
 * Provides endpoints for the core matching functionality.
 * Uses FIFO queue system - first user to start waiting gets matched first.
 * Allows authenticated users to find and be matched with compatible partners.
 */
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Developer matching endpoints")
public class MatchController {

    private final MatchService matchService;
    private final UserService userService;

    /**
     * Initiates the matching process or joins the waiting queue.
     * 
     * Business Intent (FIFO Queue):
     * - If a compatible partner is waiting → creates match immediately
     * - If no partner available → adds user to waiting queue
     * 
     * The queue follows FIFO principle - first user to wait gets matched first
     * when a compatible partner becomes available.
     *
     * Prerequisites:
     * - User must be authenticated
     * - User must have selected a role (FRONTEND or BACKEND)
     * - User must not have an existing active match
     *
     * @param oauth2User The authenticated user from Spring Security context
     * @return MatchStatusResponse with either MATCHED details or WAITING status
     */
    @PostMapping("/find")
    @Operation(
        summary = "Find a match or join queue",
        description = "Attempts to match the user with a compatible waiting partner. " +
                      "If no partner is available, the user joins the waiting queue (FIFO). " +
                      "Returns MATCHED status with match details, or WAITING status with queue position."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Match found or joined waiting queue",
            content = @Content(schema = @Schema(implementation = MatchStatusResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "User has not selected a role",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409",
            description = "User already has an active match",
            content = @Content
        )
    })
    public ResponseEntity<MatchStatusResponse> findMatch(@AuthenticationPrincipal OAuth2User oauth2User) {
        // Extract GitHub login from OAuth2 user and construct GitHub URL
        String githubLogin = oauth2User.getAttribute("login");
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + githubLogin;

        // Find user by GitHub URL to get their UUID
        UserResponse currentUser = userService.findByGithubUrl(githubUrl);

        // Initiate matching or join queue
        MatchStatusResponse response = matchService.findOrQueueMatch(currentUser.id());

        return ResponseEntity.ok(response);
    }

    /**
     * Cancels waiting in the queue.
     * 
     * Business Intent:
     * Allows users to leave the waiting queue if they no longer want to be matched.
     * Does nothing if user wasn't in the queue.
     *
     * @param oauth2User The authenticated user from Spring Security context
     * @return 204 No Content on success
     */
    @DeleteMapping("/queue")
    @Operation(
        summary = "Leave the waiting queue",
        description = "Removes the current user from the matching queue. " +
                      "Use this if you no longer want to wait for a match."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Successfully left the queue (or wasn't in queue)"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content
        )
    })
    public ResponseEntity<Void> cancelWaiting(@AuthenticationPrincipal OAuth2User oauth2User) {
        String githubLogin = oauth2User.getAttribute("login");
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + githubLogin;

        UserResponse currentUser = userService.findByGithubUrl(githubUrl);
        matchService.cancelWaiting(currentUser.id());

        return ResponseEntity.noContent().build();
    }

    /**
     * Completes an active match.
     * 
     * Business Intent:
     * Marks a match as COMPLETED and optionally saves the project repository URL.
     * After completion, both participants are freed to search for new matches.
     * 
     * Security:
     * Only participants of the match can complete it.
     *
     * @param matchId The UUID of the match to complete
     * @param request Optional request body with GitHub repo URL
     * @param oauth2User The authenticated user from Spring Security context
     * @return MatchCompletionResponse with completion details
     */
    @PostMapping("/{matchId}/complete")
    @Operation(
        summary = "Complete an active match",
        description = "Marks the specified match as COMPLETED. " +
                      "Optionally accepts a GitHub repository URL for the completed project. " +
                      "After completion, both users are free to search for new matches. " +
                      "Only participants of the match can complete it."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Match completed successfully",
            content = @Content(schema = @Schema(implementation = MatchCompletionResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Match is not in ACTIVE status",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "User is not a participant of this match",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Match not found",
            content = @Content
        )
    })
    public ResponseEntity<MatchCompletionResponse> completeMatch(
            @Parameter(description = "The UUID of the match to complete")
            @PathVariable UUID matchId,
            @Valid @RequestBody(required = false) MatchCompletionRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        // Extract GitHub login from OAuth2 user and construct GitHub URL
        String githubLogin = oauth2User.getAttribute("login");
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + githubLogin;

        // Find user by GitHub URL to get their UUID
        UserResponse currentUser = userService.findByGithubUrl(githubUrl);

        // Complete the match
        MatchCompletionResponse response = matchService.completeMatch(matchId, request, currentUser.id());

        return ResponseEntity.ok(response);
    }
}
