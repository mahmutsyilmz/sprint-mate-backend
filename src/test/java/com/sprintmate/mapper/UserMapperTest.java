package com.sprintmate.mapper;

import com.sprintmate.dto.UserResponse;
import com.sprintmate.model.RoleName;
import com.sprintmate.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserMapper.
 * Tests entity-to-DTO conversion logic.
 */
class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
    }

    @Test
    void should_MapUserToResponse_When_AllFieldsPresent() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Set<String> skills = Set.of("Java", "Spring Boot", "React");

        User user = User.builder()
                .id(userId)
                .githubUrl("https://github.com/johndoe")
                .name("John")
                .surname("Doe")
                .role(RoleName.BACKEND)
                .bio("Senior Backend Developer")
                .skills(new HashSet<>(skills))
                .build();

        // Act
        UserResponse response = userMapper.toResponse(user);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.githubUrl()).isEqualTo("https://github.com/johndoe");
        assertThat(response.name()).isEqualTo("John");
        assertThat(response.surname()).isEqualTo("Doe");
        assertThat(response.role()).isEqualTo("BACKEND");
        assertThat(response.bio()).isEqualTo("Senior Backend Developer");
        assertThat(response.skills()).containsExactlyInAnyOrderElementsOf(skills);
    }

    @Test
    void should_HandleNullRole_When_MappingUser() {
        // Arrange
        User user = User.builder()
                .id(UUID.randomUUID())
                .githubUrl("https://github.com/newuser")
                .name("New User")
                .role(null)  // User hasn't selected role yet
                .skills(new HashSet<>())
                .build();

        // Act
        UserResponse response = userMapper.toResponse(user);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.role()).isNull();
        assertThat(response.name()).isEqualTo("New User");
    }

    @Test
    void should_HandleNullSkills_When_MappingUser() {
        // Arrange
        User user = User.builder()
                .id(UUID.randomUUID())
                .githubUrl("https://github.com/testuser")
                .name("Test User")
                .role(RoleName.FRONTEND)
                .skills(null)  // No skills set
                .build();

        // Act
        UserResponse response = userMapper.toResponse(user);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.skills()).isNotNull();
        assertThat(response.skills()).isEmpty();
    }

    @Test
    void should_ReturnImmutableSkillsSet_When_Mapping() {
        // Arrange
        Set<String> originalSkills = new HashSet<>(Set.of("TypeScript", "React"));
        User user = User.builder()
                .id(UUID.randomUUID())
                .githubUrl("https://github.com/frontenddev")
                .name("Frontend Dev")
                .role(RoleName.FRONTEND)
                .skills(originalSkills)
                .build();

        // Act
        UserResponse response = userMapper.toResponse(user);

        // Assert - Verify defensive copy (modifying response shouldn't affect original)
        assertThat(response.skills()).containsExactlyInAnyOrder("TypeScript", "React");

        // Modify the response skills
        response.skills().add("Vue.js");

        // Original user skills should remain unchanged
        assertThat(user.getSkills()).containsExactlyInAnyOrder("TypeScript", "React");
        assertThat(user.getSkills()).doesNotContain("Vue.js");
    }

    @Test
    void should_MapRoleToString_When_RoleExists() {
        // Arrange
        User frontendUser = User.builder()
                .id(UUID.randomUUID())
                .githubUrl("https://github.com/fe")
                .name("FE Dev")
                .role(RoleName.FRONTEND)
                .skills(new HashSet<>())
                .build();

        User backendUser = User.builder()
                .id(UUID.randomUUID())
                .githubUrl("https://github.com/be")
                .name("BE Dev")
                .role(RoleName.BACKEND)
                .skills(new HashSet<>())
                .build();

        // Act
        UserResponse feResponse = userMapper.toResponse(frontendUser);
        UserResponse beResponse = userMapper.toResponse(backendUser);

        // Assert
        assertThat(feResponse.role()).isEqualTo("FRONTEND");
        assertThat(beResponse.role()).isEqualTo("BACKEND");
    }

    @Test
    void should_HandleNullSurname_When_MappingUser() {
        // Arrange
        User user = User.builder()
                .id(UUID.randomUUID())
                .githubUrl("https://github.com/john")
                .name("John")
                .surname(null)
                .role(RoleName.BACKEND)
                .skills(new HashSet<>())
                .build();

        // Act
        UserResponse response = userMapper.toResponse(user);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.surname()).isNull();
        assertThat(response.name()).isEqualTo("John");
    }

    @Test
    void should_HandleNullBio_When_MappingUser() {
        // Arrange
        User user = User.builder()
                .id(UUID.randomUUID())
                .githubUrl("https://github.com/user")
                .name("User")
                .bio(null)
                .role(RoleName.FRONTEND)
                .skills(new HashSet<>())
                .build();

        // Act
        UserResponse response = userMapper.toResponse(user);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.bio()).isNull();
    }

    @Test
    void should_PreserveAllDataIntegrity_When_MappingComplexUser() {
        // Arrange - Complex user with all fields populated
        UUID userId = UUID.randomUUID();
        Set<String> complexSkills = Set.of("Java", "Spring Boot", "PostgreSQL", "Docker", "Kubernetes");

        User user = User.builder()
                .id(userId)
                .githubUrl("https://github.com/senior-dev")
                .name("Senior")
                .surname("Developer")
                .role(RoleName.BACKEND)
                .bio("10+ years experience in backend development")
                .skills(new HashSet<>(complexSkills))
                .build();

        // Act
        UserResponse response = userMapper.toResponse(user);

        // Assert - Verify all fields are correctly mapped
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.githubUrl()).isEqualTo("https://github.com/senior-dev");
        assertThat(response.name()).isEqualTo("Senior");
        assertThat(response.surname()).isEqualTo("Developer");
        assertThat(response.role()).isEqualTo("BACKEND");
        assertThat(response.bio()).isEqualTo("10+ years experience in backend development");
        assertThat(response.skills()).hasSize(5);
        assertThat(response.skills()).containsExactlyInAnyOrderElementsOf(complexSkills);
    }
}
