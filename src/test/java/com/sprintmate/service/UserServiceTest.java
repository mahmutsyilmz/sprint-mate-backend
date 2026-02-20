package com.sprintmate.service;

import com.sprintmate.dto.UserResponse;
import com.sprintmate.dto.UserStatusResponse;
import com.sprintmate.dto.UserUpdateRequest;
import com.sprintmate.exception.InvalidRoleException;
import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.mapper.UserMapper;
import com.sprintmate.model.*;
import com.sprintmate.repository.MatchParticipantRepository;
import com.sprintmate.repository.MatchProjectRepository;
import com.sprintmate.repository.MatchRepository;
import com.sprintmate.repository.ProjectThemeRepository;
import com.sprintmate.repository.UserPreferenceRepository;
import com.sprintmate.repository.UserRepository;
import com.sprintmate.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Tests business logic for user management operations.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private MatchProjectRepository matchProjectRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private ProjectThemeRepository projectThemeRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = TestDataBuilder.buildUser(RoleName.BACKEND);
        testUser.setId(testUserId);
    }

    @Test
    void should_UpdateUserRole_When_ValidRoleProvided() {
        // Arrange
        String roleName = "FRONTEND";
        UserResponse expectedResponse = new UserResponse(
                testUserId,
                testUser.getGithubUrl(),
                testUser.getName(),
                testUser.getSurname(),
                "FRONTEND",
                testUser.getBio(),
                testUser.getSkills(),
                null
        );

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(expectedResponse);

        // Act
        UserResponse response = userService.updateUserRole(testUserId, roleName);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo("FRONTEND");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).findById(testUserId);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(RoleName.FRONTEND);
    }

    @Test
    void should_UpdateUserRole_When_CaseInsensitiveRole() {
        // Arrange - Test lowercase role name gets converted to uppercase
        String roleName = "backend";
        UserResponse expectedResponse = new UserResponse(
                testUserId,
                testUser.getGithubUrl(),
                testUser.getName(),
                testUser.getSurname(),
                "BACKEND",
                testUser.getBio(),
                testUser.getSkills(),
                null
        );

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(expectedResponse);

        // Act
        UserResponse response = userService.updateUserRole(testUserId, roleName);

        // Assert
        assertThat(response).isNotNull();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(RoleName.BACKEND);
    }

    @Test
    void should_ThrowResourceNotFoundException_When_UserNotFound() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserRole(nonExistentUserId, "FRONTEND"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(nonExistentUserId.toString());

        verify(userRepository).findById(nonExistentUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void should_ThrowInvalidRoleException_When_InvalidRole() {
        // Arrange
        String invalidRole = "DESIGNER";
        // No need to mock userRepository - exception thrown before repository access

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserRole(testUserId, invalidRole))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("Invalid role")
                .hasMessageContaining("DESIGNER");

        // Verify no repository interaction happened (role validation happens first)
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void should_FindUserByGithubUrl_When_UserExists() {
        // Arrange
        String githubUrl = "https://github.com/testuser";
        UserResponse expectedResponse = new UserResponse(
                testUserId,
                githubUrl,
                testUser.getName(),
                testUser.getSurname(),
                "BACKEND",
                testUser.getBio(),
                testUser.getSkills(),
                null
        );

        when(userRepository.findByGithubUrl(githubUrl)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(expectedResponse);

        // Act
        UserResponse response = userService.findByGithubUrl(githubUrl);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.githubUrl()).isEqualTo(githubUrl);
        verify(userRepository).findByGithubUrl(githubUrl);
    }

    @Test
    void should_ThrowResourceNotFoundException_When_GithubUrlNotFound() {
        // Arrange
        String nonExistentGithubUrl = "https://github.com/nonexistent";
        when(userRepository.findByGithubUrl(nonExistentGithubUrl)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findByGithubUrl(nonExistentGithubUrl))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(nonExistentGithubUrl);
    }

    @Test
    void should_UpdateAllFields_When_ValidProfileUpdate() {
        // Arrange
        Set<String> newSkills = Set.of("Java", "Spring Boot", "PostgreSQL");
        UserUpdateRequest request = new UserUpdateRequest(
                "John Updated",
                "Senior Backend Developer",
                "FRONTEND",
                newSkills,
                null
        );

        testUser.setSkills(new HashSet<>(Set.of("Old Skill")));

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(mock(UserResponse.class));

        // Act
        userService.updateUserProfile(testUserId, request);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo("John Updated");
        assertThat(savedUser.getBio()).isEqualTo("Senior Backend Developer");
        assertThat(savedUser.getRole()).isEqualTo(RoleName.FRONTEND);
        assertThat(savedUser.getSkills()).containsExactlyInAnyOrder("Java", "Spring Boot", "PostgreSQL");
    }

    @Test
    void should_PreserveRole_When_RoleNotProvided() {
        // Arrange - Validates null safety fix (Bug #10)
        UserUpdateRequest request = new UserUpdateRequest(
                "John",
                "Developer",
                null, // No role provided
                null,
                null
        );

        RoleName originalRole = testUser.getRole();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(mock(UserResponse.class));

        // Act
        userService.updateUserProfile(testUserId, request);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo("John");
        assertThat(savedUser.getBio()).isEqualTo("Developer");
        assertThat(savedUser.getRole()).isEqualTo(originalRole); // Role preserved
    }

    @Test
    void should_UpdateNameOnly_When_OnlyNameProvided() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(
                "New Name",
                null, // Bio not provided
                null,
                null,
                null
        );

        String originalBio = "Original Bio";
        testUser.setBio(originalBio);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(mock(UserResponse.class));

        // Act
        userService.updateUserProfile(testUserId, request);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo("New Name");
        assertThat(savedUser.getBio()).isEqualTo(originalBio); // Bio preserved
    }

    @Test
    void should_UpdateBioOnly_When_OnlyBioProvided() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(
                "Required Name", // Name is required in DTO validation
                "New bio text",
                null,
                null,
                null
        );

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(mock(UserResponse.class));

        // Act
        userService.updateUserProfile(testUserId, request);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo("Required Name");
        assertThat(savedUser.getBio()).isEqualTo("New bio text");
    }

    @Test
    void should_ReplaceSkills_When_SkillsProvided() {
        // Arrange
        Set<String> oldSkills = new HashSet<>(Set.of("Old1", "Old2"));
        Set<String> newSkills = Set.of("New1", "New2", "New3");
        testUser.setSkills(oldSkills);

        UserUpdateRequest request = new UserUpdateRequest(
                "John",
                null,
                null,
                newSkills,
                null
        );

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(mock(UserResponse.class));

        // Act
        userService.updateUserProfile(testUserId, request);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getSkills()).containsExactlyInAnyOrder("New1", "New2", "New3");
        assertThat(savedUser.getSkills()).doesNotContain("Old1", "Old2");
    }

    @Test
    void should_ReturnStatusWithoutMatch_When_NoActiveMatch() {
        // Arrange
        String githubUrl = "https://github.com/testuser";
        testUser.setGithubUrl(githubUrl);

        when(userRepository.findByGithubUrl(githubUrl)).thenReturn(Optional.of(testUser));
        when(matchRepository.findMatchByUserIdAndStatus(testUserId, MatchStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // Act
        UserStatusResponse response = userService.getUserStatus(githubUrl);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testUserId);
        assertThat(response.githubUrl()).isEqualTo(githubUrl);
        assertThat(response.hasActiveMatch()).isFalse();
        assertThat(response.activeMatch()).isNull();

        verify(matchParticipantRepository, never()).findByMatch(any());
        verify(matchProjectRepository, never()).findByMatch(any());
    }

    @Test
    void should_ReturnStatusWithMatch_When_ActiveMatchExists() {
        // Arrange
        String githubUrl = "https://github.com/testuser";
        testUser.setGithubUrl(githubUrl);

        UUID partnerId = UUID.randomUUID();
        User partner = TestDataBuilder.buildUser(RoleName.FRONTEND);
        partner.setId(partnerId);
        partner.setName("Partner");
        partner.setSurname("User");
        partner.setSkills(new HashSet<>(Set.of("React", "TypeScript")));

        UUID matchId = UUID.randomUUID();
        Match activeMatch = Match.builder()
                .id(matchId)
                .status(MatchStatus.ACTIVE)
                .communicationLink("https://discord.gg/test")
                .build();

        MatchParticipant participant1 = MatchParticipant.builder()
                .match(activeMatch)
                .user(testUser)
                .build();

        MatchParticipant participant2 = MatchParticipant.builder()
                .match(activeMatch)
                .user(partner)
                .build();

        ProjectTemplate template = ProjectTemplate.builder()
                .title("Todo App")
                .description("Build a task management application")
                .build();

        MatchProject matchProject = MatchProject.builder()
                .match(activeMatch)
                .projectTemplate(template)
                .build();

        when(userRepository.findByGithubUrl(githubUrl)).thenReturn(Optional.of(testUser));
        when(matchRepository.findMatchByUserIdAndStatus(testUserId, MatchStatus.ACTIVE))
                .thenReturn(Optional.of(activeMatch));
        when(matchParticipantRepository.findByMatch(activeMatch))
                .thenReturn(List.of(participant1, participant2));
        when(matchProjectRepository.findByMatch(activeMatch))
                .thenReturn(Optional.of(matchProject));

        // Act
        UserStatusResponse response = userService.getUserStatus(githubUrl);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.hasActiveMatch()).isTrue();
        assertThat(response.activeMatch()).isNotNull();
        assertThat(response.activeMatch().matchId()).isEqualTo(matchId);
        assertThat(response.activeMatch().communicationLink()).isEqualTo("https://discord.gg/test");
        assertThat(response.activeMatch().partnerName()).isEqualTo("Partner User");
        assertThat(response.activeMatch().partnerRole()).isEqualTo("FRONTEND");
        assertThat(response.activeMatch().partnerSkills()).containsExactlyInAnyOrder("React", "TypeScript");
        assertThat(response.activeMatch().projectTitle()).isEqualTo("Todo App");
        assertThat(response.activeMatch().projectDescription()).isEqualTo("Build a task management application");
    }

    @Test
    void should_BuildPartnerNameWithSurname_When_SurnameExists() {
        // Arrange
        String githubUrl = "https://github.com/testuser";
        testUser.setGithubUrl(githubUrl);

        User partner = TestDataBuilder.buildUser(RoleName.FRONTEND);
        partner.setName("John");
        partner.setSurname("Doe");

        Match activeMatch = TestDataBuilder.buildMatch(MatchStatus.ACTIVE);

        MatchParticipant participant1 = MatchParticipant.builder()
                .match(activeMatch)
                .user(testUser)
                .build();

        MatchParticipant participant2 = MatchParticipant.builder()
                .match(activeMatch)
                .user(partner)
                .build();

        when(userRepository.findByGithubUrl(githubUrl)).thenReturn(Optional.of(testUser));
        when(matchRepository.findMatchByUserIdAndStatus(testUser.getId(), MatchStatus.ACTIVE))
                .thenReturn(Optional.of(activeMatch));
        when(matchParticipantRepository.findByMatch(activeMatch))
                .thenReturn(List.of(participant1, participant2));
        when(matchProjectRepository.findByMatch(activeMatch))
                .thenReturn(Optional.empty());

        // Act
        UserStatusResponse response = userService.getUserStatus(githubUrl);

        // Assert
        assertThat(response.activeMatch()).isNotNull();
        assertThat(response.activeMatch().partnerName()).isEqualTo("John Doe");
    }

    @Test
    void should_BuildPartnerNameWithoutSurname_When_SurnameNull() {
        // Arrange
        String githubUrl = "https://github.com/testuser";
        testUser.setGithubUrl(githubUrl);

        User partner = TestDataBuilder.buildUser(RoleName.FRONTEND);
        partner.setName("John");
        partner.setSurname(null); // No surname

        Match activeMatch = TestDataBuilder.buildMatch(MatchStatus.ACTIVE);

        MatchParticipant participant1 = MatchParticipant.builder()
                .match(activeMatch)
                .user(testUser)
                .build();

        MatchParticipant participant2 = MatchParticipant.builder()
                .match(activeMatch)
                .user(partner)
                .build();

        when(userRepository.findByGithubUrl(githubUrl)).thenReturn(Optional.of(testUser));
        when(matchRepository.findMatchByUserIdAndStatus(testUser.getId(), MatchStatus.ACTIVE))
                .thenReturn(Optional.of(activeMatch));
        when(matchParticipantRepository.findByMatch(activeMatch))
                .thenReturn(List.of(participant1, participant2));
        when(matchProjectRepository.findByMatch(activeMatch))
                .thenReturn(Optional.empty());

        // Act
        UserStatusResponse response = userService.getUserStatus(githubUrl);

        // Assert
        assertThat(response.activeMatch()).isNotNull();
        assertThat(response.activeMatch().partnerName()).isEqualTo("John");
    }
}
