package com.sprintmate.service;

import com.sprintmate.dto.UserResponse;
import com.sprintmate.exception.InvalidRoleException;
import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.mapper.UserMapper;
import com.sprintmate.model.RoleName;
import com.sprintmate.model.User;
import com.sprintmate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Tests business logic in isolation using Mockito for dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private UUID testUserId;
    private User testUser;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
            .id(testUserId)
            .githubUrl("https://github.com/testuser")
            .name("Test User")
            .surname("Tester")
            .role(null)
            .build();

        testUserResponse = new UserResponse(
            testUserId,
            "https://github.com/testuser",
            "Test User",
            "Tester",
            null
        );
    }

    @Nested
    @DisplayName("updateUserRole Tests")
    class UpdateUserRoleTests {

        @Test
        @DisplayName("should_UpdateRole_When_ValidRequest")
        void should_UpdateRole_When_ValidRequest() {
            // Arrange
            UserResponse updatedResponse = new UserResponse(
                testUserId, "https://github.com/testuser", "Test User", "Tester", "FRONTEND"
            );

            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userMapper.toResponse(any(User.class))).thenReturn(updatedResponse);

            // Act
            UserResponse result = userService.updateUserRole(testUserId, "FRONTEND");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(testUserId);
            assertThat(result.role()).isEqualTo("FRONTEND");
            assertThat(result.githubUrl()).isEqualTo("https://github.com/testuser");
            assertThat(result.name()).isEqualTo("Test User");

            verify(userRepository).findById(testUserId);
            verify(userRepository).save(any(User.class));
            verify(userMapper).toResponse(any(User.class));
        }

        @Test
        @DisplayName("should_UpdateRole_When_BackendRoleProvided")
        void should_UpdateRole_When_BackendRoleProvided() {
            // Arrange
            UserResponse backendResponse = new UserResponse(
                testUserId, "https://github.com/testuser", "Test User", "Tester", "BACKEND"
            );

            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userMapper.toResponse(any(User.class))).thenReturn(backendResponse);

            // Act
            UserResponse result = userService.updateUserRole(testUserId, "BACKEND");

            // Assert
            assertThat(result.role()).isEqualTo("BACKEND");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should_UpdateRole_When_LowercaseRoleProvided")
        void should_UpdateRole_When_LowercaseRoleProvided() {
            // Arrange
            UserResponse frontendResponse = new UserResponse(
                testUserId, "https://github.com/testuser", "Test User", "Tester", "FRONTEND"
            );

            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userMapper.toResponse(any(User.class))).thenReturn(frontendResponse);

            // Act
            UserResponse result = userService.updateUserRole(testUserId, "frontend");

            // Assert
            assertThat(result.role()).isEqualTo("FRONTEND");
        }

        @Test
        @DisplayName("should_ThrowException_When_InvalidRole")
        void should_ThrowException_When_InvalidRole() {
            // Arrange - no repository setup needed as validation happens first

            // Act & Assert
            assertThatThrownBy(() -> userService.updateUserRole(testUserId, "INVALID_ROLE"))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("Invalid role: 'INVALID_ROLE'");

            verify(userRepository, never()).findById(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_ThrowException_When_EmptyRoleProvided")
        void should_ThrowException_When_EmptyRoleProvided() {
            // Act & Assert
            assertThatThrownBy(() -> userService.updateUserRole(testUserId, ""))
                .isInstanceOf(InvalidRoleException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_ThrowException_When_UserNotFound")
        void should_ThrowException_When_UserNotFound() {
            // Arrange
            UUID nonExistentUserId = UUID.randomUUID();
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.updateUserRole(nonExistentUserId, "FRONTEND"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

            verify(userRepository).findById(nonExistentUserId);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_OverwriteExistingRole_When_UserAlreadyHasRole")
        void should_OverwriteExistingRole_When_UserAlreadyHasRole() {
            // Arrange
            testUser.setRole(RoleName.FRONTEND);
            UserResponse backendResponse = new UserResponse(
                testUserId, "https://github.com/testuser", "Test User", "Tester", "BACKEND"
            );

            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userMapper.toResponse(any(User.class))).thenReturn(backendResponse);

            // Act
            UserResponse result = userService.updateUserRole(testUserId, "BACKEND");

            // Assert
            assertThat(result.role()).isEqualTo("BACKEND");
        }
    }

    @Nested
    @DisplayName("findByGithubUrl Tests")
    class FindByGithubUrlTests {

        @Test
        @DisplayName("should_ReturnUser_When_UserExists")
        void should_ReturnUser_When_UserExists() {
            // Arrange
            String githubUrl = "https://github.com/testuser";
            testUser.setRole(RoleName.FRONTEND);
            UserResponse frontendResponse = new UserResponse(
                testUserId, githubUrl, "Test User", "Tester", "FRONTEND"
            );

            when(userRepository.findByGithubUrl(githubUrl)).thenReturn(Optional.of(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(frontendResponse);

            // Act
            UserResponse result = userService.findByGithubUrl(githubUrl);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.githubUrl()).isEqualTo(githubUrl);
            assertThat(result.name()).isEqualTo("Test User");
            assertThat(result.role()).isEqualTo("FRONTEND");

            verify(userRepository).findByGithubUrl(githubUrl);
            verify(userMapper).toResponse(testUser);
        }

        @Test
        @DisplayName("should_ThrowException_When_UserNotFound")
        void should_ThrowException_When_UserNotFound() {
            // Arrange
            String nonExistentUrl = "https://github.com/nonexistent";
            when(userRepository.findByGithubUrl(nonExistentUrl)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.findByGithubUrl(nonExistentUrl))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

            verify(userRepository).findByGithubUrl(nonExistentUrl);
        }

        @Test
        @DisplayName("should_ReturnNullRole_When_UserHasNoRole")
        void should_ReturnNullRole_When_UserHasNoRole() {
            // Arrange
            String githubUrl = "https://github.com/testuser";
            testUser.setRole(null);

            when(userRepository.findByGithubUrl(githubUrl)).thenReturn(Optional.of(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            // Act
            UserResponse result = userService.findByGithubUrl(githubUrl);

            // Assert
            assertThat(result.role()).isNull();
        }
    }
}
