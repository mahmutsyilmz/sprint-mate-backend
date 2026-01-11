package com.sprintmate.controller;

import com.sprintmate.dto.MatchCompletionRequest;
import com.sprintmate.dto.MatchCompletionResponse;
import com.sprintmate.dto.MatchStatusResponse;
import com.sprintmate.dto.UserResponse;
import com.sprintmate.exception.*;
import com.sprintmate.service.MatchService;
import com.sprintmate.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MatchController.
 * Tests HTTP layer including request/response handling and security.
 */
@WebMvcTest(MatchController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("MatchController Tests")
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @MockBean
    private UserService userService;

    private UUID testUserId;
    private UserResponse testUserResponse;
    private MatchStatusResponse matchedResponse;
    private MatchStatusResponse waitingResponse;
    private OAuth2User mockOAuth2User;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        testUserResponse = new UserResponse(
            testUserId,
            "https://github.com/testuser",
            "Test User",
            "Tester",
            "FRONTEND",
            null,
            new HashSet<>()
        );

        matchedResponse = MatchStatusResponse.matched(
            UUID.randomUUID(),
            "https://meet.google.com/mock-id",
            "Partner Developer",
            "BACKEND",
            "E-Commerce MVP",
            "Build a full-stack e-commerce application"
        );

        waitingResponse = MatchStatusResponse.waiting(
            LocalDateTime.now(),
            1
        );

        // Create mock OAuth2User with GitHub attributes
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("login", "testuser");
        attributes.put("name", "Test User");
        mockOAuth2User = new DefaultOAuth2User(
            Collections.emptyList(),
            attributes,
            "login"
        );
    }

    @Nested
    @DisplayName("POST /api/matches/find Tests")
    class FindMatchTests {

        @Test
        @DisplayName("should_Return200WithMatched_When_PartnerFound")
        void should_Return200WithMatched_When_PartnerFound() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(testUserResponse);
            when(matchService.findOrQueueMatch(testUserId))
                .thenReturn(matchedResponse);

            // Act & Assert
            mockMvc.perform(post("/api/matches/find")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("MATCHED"))
                .andExpect(jsonPath("$.matchId").exists())
                .andExpect(jsonPath("$.meetingUrl").value("https://meet.google.com/mock-id"))
                .andExpect(jsonPath("$.partnerName").value("Partner Developer"))
                .andExpect(jsonPath("$.partnerRole").value("BACKEND"))
                .andExpect(jsonPath("$.projectTitle").value("E-Commerce MVP"));
        }

        @Test
        @DisplayName("should_Return200WithWaiting_When_NoPartnerAvailable")
        void should_Return200WithWaiting_When_NoPartnerAvailable() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(testUserResponse);
            when(matchService.findOrQueueMatch(testUserId))
                .thenReturn(waitingResponse);

            // Act & Assert
            mockMvc.perform(post("/api/matches/find")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.matchId").doesNotExist())
                .andExpect(jsonPath("$.queuePosition").value(1))
                .andExpect(jsonPath("$.waitingSince").exists());
        }

        @Test
        @DisplayName("should_Return400_When_UserHasNoRole")
        void should_Return400_When_UserHasNoRole() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(testUserResponse);
            when(matchService.findOrQueueMatch(testUserId))
                .thenThrow(RoleNotSelectedException.forUser(testUserId));

            // Act & Assert
            mockMvc.perform(post("/api/matches/find")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("should_Return302_When_NotAuthenticated")
        void should_Return302_When_NotAuthenticated() throws Exception {
            // Act & Assert - No oauth2Login() = unauthenticated (redirects to login)
            mockMvc.perform(post("/api/matches/find")
                    .with(csrf()))
                .andExpect(status().isFound()); // 302 redirect to OAuth2 login
        }

        @Test
        @DisplayName("should_Return404_When_UserNotFound")
        void should_Return404_When_UserNotFound() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenThrow(new ResourceNotFoundException("User", "githubUrl", "https://github.com/testuser"));

            // Act & Assert
            mockMvc.perform(post("/api/matches/find")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("should_Return409_When_UserAlreadyHasActiveMatch")
        void should_Return409_When_UserAlreadyHasActiveMatch() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(testUserResponse);
            when(matchService.findOrQueueMatch(testUserId))
                .thenThrow(ActiveMatchExistsException.forUser(testUserId));

            // Act & Assert
            mockMvc.perform(post("/api/matches/find")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /api/matches/queue Tests")
    class CancelWaitingTests {

        @Test
        @DisplayName("should_Return204_When_CancelWaitingSuccessful")
        void should_Return204_When_CancelWaitingSuccessful() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(testUserResponse);
            doNothing().when(matchService).cancelWaiting(testUserId);

            // Act & Assert
            mockMvc.perform(delete("/api/matches/queue")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf()))
                .andExpect(status().isNoContent());

            verify(matchService).cancelWaiting(testUserId);
        }

        @Test
        @DisplayName("should_Return302_When_NotAuthenticated")
        void should_Return302_When_NotAuthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/matches/queue")
                    .with(csrf()))
                .andExpect(status().isFound()); // 302 redirect to OAuth2 login
        }
    }

    @Nested
    @DisplayName("POST /api/matches/{matchId}/complete Tests")
    class CompleteMatchTests {

        private UUID matchId;
        private MatchCompletionResponse completionResponse;
        private MatchCompletionResponse completionResponseWithRepo;

        @BeforeEach
        void setUpCompleteMatchTests() {
            matchId = UUID.randomUUID();
            completionResponse = MatchCompletionResponse.of(matchId, LocalDateTime.now(), null);
            completionResponseWithRepo = MatchCompletionResponse.of(matchId, LocalDateTime.now(), "https://github.com/team/project");
        }

        @Test
        @DisplayName("should_Return200_When_MatchCompletedSuccessfully")
        void should_Return200_When_MatchCompletedSuccessfully() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(testUserResponse);
            when(matchService.completeMatch(eq(matchId), any(MatchCompletionRequest.class), eq(testUserId)))
                .thenReturn(completionResponseWithRepo);

            // Act & Assert
            mockMvc.perform(post("/api/matches/{matchId}/complete", matchId)
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"githubRepoUrl\": \"https://github.com/team/project\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists())
                .andExpect(jsonPath("$.repoUrl").value("https://github.com/team/project"));
        }

        @Test
        @DisplayName("should_Return200_When_CompletedWithoutRepoUrl")
        void should_Return200_When_CompletedWithoutRepoUrl() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(testUserResponse);
            when(matchService.completeMatch(eq(matchId), any(), eq(testUserId)))
                .thenReturn(completionResponse);

            // Act & Assert - Empty body
            mockMvc.perform(post("/api/matches/{matchId}/complete", matchId)
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.repoUrl").doesNotExist());
        }

        @Test
        @DisplayName("should_Return403_When_UserNotParticipant")
        void should_Return403_When_UserNotParticipant() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(testUserResponse);
            when(matchService.completeMatch(eq(matchId), any(), eq(testUserId)))
                .thenThrow(new AccessDeniedException("User is not authorized to complete match"));

            // Act & Assert
            mockMvc.perform(post("/api/matches/{matchId}/complete", matchId)
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"githubRepoUrl\": \"https://github.com/team/project\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("should_Return400_When_MatchAlreadyCompleted")
        void should_Return400_When_MatchAlreadyCompleted() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(testUserResponse);
            when(matchService.completeMatch(eq(matchId), any(), eq(testUserId)))
                .thenThrow(new IllegalStateException("Match cannot be completed. Current status: COMPLETED"));

            // Act & Assert
            mockMvc.perform(post("/api/matches/{matchId}/complete", matchId)
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"githubRepoUrl\": \"https://github.com/team/project\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("should_Return404_When_MatchNotFound")
        void should_Return404_When_MatchNotFound() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(testUserResponse);
            when(matchService.completeMatch(eq(matchId), any(), eq(testUserId)))
                .thenThrow(new ResourceNotFoundException("Match", "id", matchId));

            // Act & Assert
            mockMvc.perform(post("/api/matches/{matchId}/complete", matchId)
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"githubRepoUrl\": \"https://github.com/team/project\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("should_Return302_When_NotAuthenticated")
        void should_Return302_When_NotAuthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/matches/{matchId}/complete", matchId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"githubRepoUrl\": \"https://github.com/team/project\"}"))
                .andExpect(status().isFound()); // 302 redirect to OAuth2 login
        }

        @Test
        @DisplayName("should_Return400_When_InvalidRepoUrl")
        void should_Return400_When_InvalidRepoUrl() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(testUserResponse);

            // Act & Assert - Invalid URL format
            mockMvc.perform(post("/api/matches/{matchId}/complete", matchId)
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"githubRepoUrl\": \"not-a-valid-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
        }
    }
}
