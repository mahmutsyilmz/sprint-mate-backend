package com.sprintmate.controller;

import com.sprintmate.constant.GitHubConstants;
import com.sprintmate.dto.RoleSelectionRequest;
import com.sprintmate.dto.UserResponse;
import com.sprintmate.dto.UserStatusResponse;
import com.sprintmate.dto.UserUpdateRequest;
import com.sprintmate.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * REST Controller for User-related operations.
 * 
 * Business Intent:
 * Provides endpoints for user profile management, including role selection.
 * All endpoints require authentication via GitHub OAuth2.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    /**
     * Updates the current user's role.
     * 
     * Business Intent:
     * Allows authenticated users to select their developer role (FRONTEND or BACKEND).
     * This is required before the user can be matched with a partner.
     * Role selection is a one-time action that determines matching compatibility.
     *
     * @param request    The role selection request containing the role name
     * @param oauth2User The authenticated user from Spring Security context
     * @return Updated user information including the new role
     */
    @PatchMapping("/me/role")
    @Operation(
        summary = "Update current user's role",
        description = "Allows the authenticated user to select their developer role (FRONTEND or BACKEND). " +
                      "This determines what type of partner they will be matched with."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role updated successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid role name provided",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content
        )
    })
    public ResponseEntity<UserResponse> updateMyRole(
            @Valid @RequestBody RoleSelectionRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        // Extract GitHub login from OAuth2 user and construct GitHub URL
        String githubLogin = oauth2User.getAttribute("login");
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + githubLogin;

        // Find user by GitHub URL to get their UUID
        UserResponse currentUser = userService.findByGithubUrl(githubUrl);

        // Update the role
        UserResponse updatedUser = userService.updateUserRole(currentUser.id(), request.roleName());

        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Gets the current authenticated user's profile.
     * 
     * Business Intent:
     * Allows users to view their own profile information.
     * Useful for displaying user info in the frontend.
     *
     * @param oauth2User The authenticated user from Spring Security context
     * @return Current user's profile information
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get current user's profile",
        description = "Returns the profile information of the currently authenticated user."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content
        )
    })
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal OAuth2User oauth2User) {
        String githubLogin = oauth2User.getAttribute("login");
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + githubLogin;

        UserResponse user = userService.findByGithubUrl(githubUrl);
        return ResponseEntity.ok(user);
    }

    /**
     * Gets the current authenticated user's complete status including active match.
     *
     * Business Intent:
     * Critical for session persistence - called on login/refresh to determine
     * if user is in an active match and should be redirected to sprint view.
     *
     * @param oauth2User The authenticated user from Spring Security context
     * @return Complete user status with active match details if applicable
     */
    @GetMapping("/me/status")
    @Operation(
        summary = "Get current user's status with active match info",
        description = "Returns the complete status of the currently authenticated user, " +
                      "including whether they have an active match and all match details. " +
                      "Use this on login/refresh to restore user state."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserStatusResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content
        )
    })
    public ResponseEntity<UserStatusResponse> getMyStatus(@AuthenticationPrincipal OAuth2User oauth2User) {
        String githubLogin = oauth2User.getAttribute("login");
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + githubLogin;

        UserStatusResponse status = userService.getUserStatus(githubUrl);
        return ResponseEntity.ok(status);
    }

    /**
     * Updates the current user's profile.
     * 
     * Business Intent:
     * Allows authenticated users to update their editable profile fields.
     * Uses @AuthenticationPrincipal to ensure users can ONLY edit their own profile.
     *
     * @param request    The update request containing new profile values
     * @param oauth2User The authenticated user from Spring Security context
     * @return Updated user information
     */
    @PutMapping("/me")
    @Operation(
        summary = "Update current user's profile",
        description = "Allows the authenticated user to update their profile details (name, bio, role). " +
                      "Users can only update their own profile. Role is optional - if not provided, the existing role is preserved."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data (validation failed)",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content
        )
    })
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        // Extract GitHub login from OAuth2 user and construct GitHub URL
        String githubLogin = oauth2User.getAttribute("login");
        String githubUrl = GitHubConstants.GITHUB_BASE_URL + githubLogin;

        // Find user by GitHub URL to get their UUID
        UserResponse currentUser = userService.findByGithubUrl(githubUrl);

        // Update the profile
        UserResponse updatedUser = userService.updateUserProfile(currentUser.id(), request);

        return ResponseEntity.ok(updatedUser);
    }
}
