package com.sprintmate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintmate.config.SecurityConfig;
import com.sprintmate.dto.RoleSelectionRequest;
import com.sprintmate.dto.UserResponse;
import com.sprintmate.dto.UserUpdateRequest;
import com.sprintmate.exception.GlobalExceptionHandler;
import com.sprintmate.exception.InvalidRoleException;
import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.service.CustomOAuth2UserService;
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
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController.
 * Tests HTTP layer including request/response handling and security.
 */
@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    private UUID testUserId;
    private UserResponse testUserResponse;
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
            null
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
    @DisplayName("PATCH /api/users/me/role Tests")
    class UpdateRoleTests {

        @Test
        @DisplayName("should_Return200_When_ValidRoleProvided")
        void should_Return200_When_ValidRoleProvided() throws Exception {
            // Arrange
            RoleSelectionRequest request = new RoleSelectionRequest("FRONTEND");
            UserResponse userBeforeUpdate = new UserResponse(
                testUserId, "https://github.com/testuser", "Test User", "Tester", null, null
            );

            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(userBeforeUpdate);
            when(userService.updateUserRole(eq(testUserId), eq("FRONTEND")))
                .thenReturn(testUserResponse);

            // Act & Assert
            mockMvc.perform(patch("/api/users/me/role")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.role").value("FRONTEND"))
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/testuser"))
                .andExpect(jsonPath("$.name").value("Test User"));
        }

        @Test
        @DisplayName("should_Return200_When_BackendRoleProvided")
        void should_Return200_When_BackendRoleProvided() throws Exception {
            // Arrange
            RoleSelectionRequest request = new RoleSelectionRequest("BACKEND");
            UserResponse backendUser = new UserResponse(
                testUserId, "https://github.com/testuser", "Test User", "Tester", "BACKEND", null
            );
            UserResponse userBeforeUpdate = new UserResponse(
                testUserId, "https://github.com/testuser", "Test User", "Tester", null, null
            );

            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(userBeforeUpdate);
            when(userService.updateUserRole(eq(testUserId), eq("BACKEND")))
                .thenReturn(backendUser);

            // Act & Assert
            mockMvc.perform(patch("/api/users/me/role")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BACKEND"));
        }

        @Test
        @DisplayName("should_Return400_When_InvalidRoleProvided")
        void should_Return400_When_InvalidRoleProvided() throws Exception {
            // Arrange - Invalid role that passes DTO validation but fails service validation
            // We need to test with a valid pattern but invalid service logic
            RoleSelectionRequest request = new RoleSelectionRequest("FRONTEND");
            UserResponse userBeforeUpdate = new UserResponse(
                testUserId, "https://github.com/testuser", "Test User", "Tester", null, null
            );

            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(userBeforeUpdate);
            when(userService.updateUserRole(any(), any()))
                .thenThrow(new InvalidRoleException("INVALID_ROLE"));

            // Act & Assert
            mockMvc.perform(patch("/api/users/me/role")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("should_Return400_When_RoleIsBlank")
        void should_Return400_When_RoleIsBlank() throws Exception {
            // Arrange - Empty role name fails DTO validation
            String invalidJson = "{\"roleName\": \"\"}";

            // Act & Assert
            mockMvc.perform(patch("/api/users/me/role")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
        }

        @Test
        @DisplayName("should_Return400_When_RoleNameInvalid")
        void should_Return400_When_RoleNameInvalid() throws Exception {
            // Arrange - Invalid role pattern fails DTO @Pattern validation
            String invalidJson = "{\"roleName\": \"INVALID\"}";

            // Act & Assert
            mockMvc.perform(patch("/api/users/me/role")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should_Return401_When_NotAuthenticated")
        void should_Return401_When_NotAuthenticated() throws Exception {
            // Arrange
            RoleSelectionRequest request = new RoleSelectionRequest("FRONTEND");

            // Act & Assert - No oauth2Login() = unauthenticated (redirects to login = 302)
            mockMvc.perform(patch("/api/users/me/role")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isFound()); // 302 redirect to OAuth2 login
        }

        @Test
        @DisplayName("should_Return404_When_UserNotFound")
        void should_Return404_When_UserNotFound() throws Exception {
            // Arrange
            RoleSelectionRequest request = new RoleSelectionRequest("FRONTEND");

            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenThrow(new ResourceNotFoundException("User", "githubUrl", "https://github.com/testuser"));

            // Act & Assert
            mockMvc.perform(patch("/api/users/me/role")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/users/me Tests")
    class GetProfileTests {

        @Test
        @DisplayName("should_Return200_When_Authenticated")
        void should_Return200_When_Authenticated() throws Exception {
            // Arrange
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(testUserResponse);

            // Act & Assert
            mockMvc.perform(get("/api/users/me")
                    .with(oauth2Login().oauth2User(mockOAuth2User)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/testuser"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.role").value("FRONTEND"));
        }

        @Test
        @DisplayName("should_Return302_When_NotAuthenticated")
        void should_Return302_When_NotAuthenticated() throws Exception {
            // Act & Assert - No oauth2Login() = unauthenticated (redirects to login)
            mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isFound()); // 302 redirect to OAuth2 login
        }

        @Test
        @DisplayName("should_ReturnNullRole_When_UserHasNoRole")
        void should_ReturnNullRole_When_UserHasNoRole() throws Exception {
            // Arrange
            UserResponse userWithNoRole = new UserResponse(
                testUserId, "https://github.com/testuser", "Test User", "Tester", null, null
            );
            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(userWithNoRole);

            // Act & Assert
            mockMvc.perform(get("/api/users/me")
                    .with(oauth2Login().oauth2User(mockOAuth2User)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").doesNotExist());
        }
    }

    @Nested
    @DisplayName("PUT /api/users/me Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("should_Return200_When_ValidProfileUpdateProvided")
        void should_Return200_When_ValidProfileUpdateProvided() throws Exception {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest("Updated Name", "Full-stack developer", null);
            UserResponse userBeforeUpdate = new UserResponse(
                testUserId, "https://github.com/testuser", "Test User", "Tester", "FRONTEND", null
            );
            UserResponse updatedUser = new UserResponse(
                testUserId, "https://github.com/testuser", "Updated Name", "Tester", "FRONTEND", "Full-stack developer"
            );

            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(userBeforeUpdate);
            when(userService.updateUserProfile(eq(testUserId), any(UserUpdateRequest.class)))
                .thenReturn(updatedUser);

            // Act & Assert
            mockMvc.perform(put("/api/users/me")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.bio").value("Full-stack developer"));
        }

        @Test
        @DisplayName("should_Return200_When_BioIsNull")
        void should_Return200_When_BioIsNull() throws Exception {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest("Updated Name", null, null);
            UserResponse userBeforeUpdate = new UserResponse(
                testUserId, "https://github.com/testuser", "Test User", "Tester", "FRONTEND", null
            );
            UserResponse updatedUser = new UserResponse(
                testUserId, "https://github.com/testuser", "Updated Name", "Tester", "FRONTEND", null
            );

            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(userBeforeUpdate);
            when(userService.updateUserProfile(eq(testUserId), any(UserUpdateRequest.class)))
                .thenReturn(updatedUser);

            // Act & Assert
            mockMvc.perform(put("/api/users/me")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.bio").doesNotExist());
        }

        @Test
        @DisplayName("should_Return200_When_RoleUpdated")
        void should_Return200_When_RoleUpdated() throws Exception {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest("Updated Name", "Backend expert", "BACKEND");
            UserResponse userBeforeUpdate = new UserResponse(
                testUserId, "https://github.com/testuser", "Test User", "Tester", "FRONTEND", null
            );
            UserResponse updatedUser = new UserResponse(
                testUserId, "https://github.com/testuser", "Updated Name", "Tester", "BACKEND", "Backend expert"
            );

            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenReturn(userBeforeUpdate);
            when(userService.updateUserProfile(eq(testUserId), any(UserUpdateRequest.class)))
                .thenReturn(updatedUser);

            // Act & Assert
            mockMvc.perform(put("/api/users/me")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.role").value("BACKEND"))
                .andExpect(jsonPath("$.bio").value("Backend expert"));
        }

        @Test
        @DisplayName("should_Return400_When_InvalidRoleProvided")
        void should_Return400_When_InvalidRoleProvided() throws Exception {
            // Arrange - Invalid role fails DTO @Pattern validation
            String invalidJson = "{\"name\": \"Valid Name\", \"bio\": \"Bio\", \"role\": \"INVALID\"}";

            // Act & Assert
            mockMvc.perform(put("/api/users/me")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.role").exists());
        }

        @Test
        @DisplayName("should_Return400_When_NameIsBlank")
        void should_Return400_When_NameIsBlank() throws Exception {
            // Arrange - Empty name fails DTO validation
            String invalidJson = "{\"name\": \"\", \"bio\": \"Some bio\"}";

            // Act & Assert
            mockMvc.perform(put("/api/users/me")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
        }

        @Test
        @DisplayName("should_Return400_When_BioTooLong")
        void should_Return400_When_BioTooLong() throws Exception {
            // Arrange - Bio exceeds 255 characters
            String longBio = "a".repeat(256);
            String invalidJson = String.format("{\"name\": \"Valid Name\", \"bio\": \"%s\"}", longBio);

            // Act & Assert
            mockMvc.perform(put("/api/users/me")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.bio").exists());
        }

        @Test
        @DisplayName("should_Return302_When_NotAuthenticated")
        void should_Return302_When_NotAuthenticated() throws Exception {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest("Updated Name", "Bio", null);

            // Act & Assert - No oauth2Login() = unauthenticated (redirects to login)
            mockMvc.perform(put("/api/users/me")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isFound()); // 302 redirect to OAuth2 login
        }

        @Test
        @DisplayName("should_Return404_When_UserNotFound")
        void should_Return404_When_UserNotFound() throws Exception {
            // Arrange
            UserUpdateRequest request = new UserUpdateRequest("Updated Name", "Bio", null);

            when(userService.findByGithubUrl("https://github.com/testuser"))
                .thenThrow(new ResourceNotFoundException("User", "githubUrl", "https://github.com/testuser"));

            // Act & Assert
            mockMvc.perform(put("/api/users/me")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("should_Return200_When_LoggingOut")
        void should_Return200_When_LoggingOut() throws Exception {
            // Act & Assert - Logout should return 200 OK (not redirect)
            mockMvc.perform(post("/api/auth/logout")
                    .with(oauth2Login().oauth2User(mockOAuth2User))
                    .with(csrf()))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should_Return200_When_LoggingOutWithoutAuthentication")
        void should_Return200_When_LoggingOutWithoutAuthentication() throws Exception {
            // Act & Assert - Logout without authentication should still work
            mockMvc.perform(post("/api/auth/logout")
                    .with(csrf()))
                .andExpect(status().isOk());
        }
    }
}
