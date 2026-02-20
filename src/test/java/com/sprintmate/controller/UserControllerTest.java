package com.sprintmate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintmate.dto.RoleSelectionRequest;
import com.sprintmate.dto.UserResponse;
import com.sprintmate.dto.UserStatusResponse;
import com.sprintmate.dto.UserUpdateRequest;
import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for UserController.
 * Tests HTTP layer validation and request/response mapping.
 *
 * Note: Uses simplified OAuth2 mocking. Full OAuth2 flow is tested in integration tests.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser
    void should_Return200_When_UpdatingRoleWithValidData() throws Exception {
        // Arrange
        RoleSelectionRequest request = new RoleSelectionRequest("FRONTEND");

        UUID userId = UUID.randomUUID();
        UserResponse mockResponse = new UserResponse(
                userId,
                "https://github.com/testuser",
                "Test User",
                null,
                "FRONTEND",
                null,
                new HashSet<>(),
                null
        );

        when(userService.findByGithubUrl(anyString())).thenReturn(mockResponse);
        when(userService.updateUserRole(any(UUID.class), anyString())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/users/me/role")
                        .with(csrf())
                        .with(oauth2Login().attributes(attrs -> attrs.put("login", "testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.role").value("FRONTEND"));

        verify(userService).findByGithubUrl("https://github.com/testuser");
        verify(userService).updateUserRole(userId, "FRONTEND");
    }

    @Test
    @WithMockUser
    void should_Return400_When_RoleNameIsInvalid() throws Exception {
        // Arrange - Empty role name should fail validation
        RoleSelectionRequest request = new RoleSelectionRequest("");

        // Act & Assert
        mockMvc.perform(patch("/api/users/me/role")
                        .with(csrf())
                        .with(oauth2Login().attributes(attrs -> attrs.put("login", "testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void should_Return404_When_UserNotFound() throws Exception {
        // Arrange
        RoleSelectionRequest request = new RoleSelectionRequest("BACKEND");

        when(userService.findByGithubUrl(anyString()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(patch("/api/users/me/role")
                        .with(csrf())
                        .with(oauth2Login().attributes(attrs -> attrs.put("login", "unknownuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void should_Return200_When_GettingUserProfile() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserResponse mockResponse = new UserResponse(
                userId,
                "https://github.com/testuser",
                "Test User",
                "Test Surname",
                "BACKEND",
                "Software Developer",
                new HashSet<>(),
                null
        );

        when(userService.findByGithubUrl("https://github.com/testuser")).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/users/me")
                        .with(oauth2Login().attributes(attrs -> attrs.put("login", "testuser"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/testuser"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.role").value("BACKEND"));

        verify(userService).findByGithubUrl("https://github.com/testuser");
    }

    @Test
    @WithMockUser
    void should_Return200_When_GettingUserStatus() throws Exception {
        // Arrange
        UserStatusResponse mockStatus = new UserStatusResponse(
                UUID.randomUUID(),
                "https://github.com/testuser",
                "Test User",
                null,
                "FRONTEND",
                null,
                new HashSet<>(),
                false,
                null
        );

        when(userService.getUserStatus("https://github.com/testuser")).thenReturn(mockStatus);

        // Act & Assert
        mockMvc.perform(get("/api/users/me/status")
                        .with(oauth2Login().attributes(attrs -> attrs.put("login", "testuser"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.role").value("FRONTEND"));

        verify(userService).getUserStatus("https://github.com/testuser");
    }

    @Test
    @WithMockUser
    void should_Return200_When_UpdatingUserProfile() throws Exception {
        // Arrange
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "Updated Name",
                "Updated bio",
                "BACKEND",
                new HashSet<>(),
                null
        );

        UUID userId = UUID.randomUUID();
        UserResponse currentUser = new UserResponse(
                userId,
                "https://github.com/testuser",
                "Old Name",
                null,
                "FRONTEND",
                null,
                new HashSet<>(),
                null
        );

        UserResponse updatedUser = new UserResponse(
                userId,
                "https://github.com/testuser",
                "Updated Name",
                null,
                "BACKEND",
                "Updated bio",
                new HashSet<>(),
                null
        );

        when(userService.findByGithubUrl("https://github.com/testuser")).thenReturn(currentUser);
        when(userService.updateUserProfile(userId, updateRequest)).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/api/users/me")
                        .with(csrf())
                        .with(oauth2Login().attributes(attrs -> attrs.put("login", "testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.bio").value("Updated bio"))
                .andExpect(jsonPath("$.role").value("BACKEND"));

        verify(userService).findByGithubUrl("https://github.com/testuser");
        verify(userService).updateUserProfile(userId, updateRequest);
    }

    @Test
    @WithMockUser
    void should_ExtractGithubUrlCorrectly_When_RequestMade() throws Exception {
        // Arrange
        String githubLogin = "myusername";
        String expectedGithubUrl = "https://github.com/" + githubLogin;

        UserResponse mockResponse = new UserResponse(
                UUID.randomUUID(),
                expectedGithubUrl,
                "User",
                null,
                null,
                null,
                new HashSet<>(),
                null
        );

        when(userService.findByGithubUrl(expectedGithubUrl)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/users/me")
                        .with(oauth2Login().attributes(attrs -> attrs.put("login", githubLogin))))
                .andExpect(status().isOk());

        verify(userService).findByGithubUrl(expectedGithubUrl);
    }
}
