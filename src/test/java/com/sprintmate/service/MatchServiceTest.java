package com.sprintmate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintmate.dto.MatchCompletionRequest;
import com.sprintmate.dto.MatchCompletionResponse;
import com.sprintmate.dto.MatchStatusResponse;
import com.sprintmate.exception.ActiveMatchExistsException;
import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.exception.RoleNotSelectedException;
import com.sprintmate.model.*;
import com.sprintmate.repository.*;
import com.sprintmate.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchService.
 * Tests complex business logic for developer matching and match completion.
 */
@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private MatchProjectRepository matchProjectRepository;

    @Mock
    private MatchCompletionRepository matchCompletionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectGeneratorService projectGeneratorService;

    @Mock
    private SprintReviewService sprintReviewService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MatchService matchService;

    private User frontendUser;
    private User backendUser;
    private UUID frontendUserId;
    private UUID backendUserId;

    @BeforeEach
    void setUp() {
        frontendUserId = UUID.randomUUID();
        backendUserId = UUID.randomUUID();

        frontendUser = TestDataBuilder.buildUser(RoleName.FRONTEND);
        frontendUser.setId(frontendUserId);
        frontendUser.setName("Frontend");
        frontendUser.setSurname("Dev");

        backendUser = TestDataBuilder.buildUser(RoleName.BACKEND);
        backendUser.setId(backendUserId);
        backendUser.setName("Backend");
        backendUser.setSurname("Dev");
    }

    @Test
    void should_ThrowResourceNotFoundException_When_UserNotFound() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> matchService.findOrQueueMatch(nonExistentUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(nonExistentUserId.toString());
    }

    @Test
    void should_ThrowRoleNotSelectedException_When_RoleNull() {
        // Arrange
        frontendUser.setRole(null); // User hasn't selected a role yet
        when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));

        // Act & Assert
        assertThatThrownBy(() -> matchService.findOrQueueMatch(frontendUserId))
                .isInstanceOf(RoleNotSelectedException.class)
                .hasMessageContaining(frontendUserId.toString())
                .hasMessageContaining("must select a role");
    }

    @Test
    void should_ThrowActiveMatchExistsException_When_AlreadyMatched() {
        // Arrange
        when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
        when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> matchService.findOrQueueMatch(frontendUserId))
                .isInstanceOf(ActiveMatchExistsException.class)
                .hasMessageContaining(frontendUserId.toString())
                .hasMessageContaining("already has an active match");
    }

    @Test
    void should_CreateMatchImmediately_When_CompatiblePartnerWaiting() {
        // Arrange
        LocalDateTime waitingSince = LocalDateTime.now().minusMinutes(5);
        backendUser.setWaitingSince(waitingSince); // Backend user is waiting

        ProjectTemplate template = TestDataBuilder.buildProjectTemplate("Todo App", "Build a task manager");
        ProjectIdea idea = ProjectIdea.builder().build();
        ProjectGeneratorService.GeneratedProject generated =
                new ProjectGeneratorService.GeneratedProject(template, idea, null, null);

        Match savedMatch = Match.builder()
                .id(UUID.randomUUID())
                .status(MatchStatus.ACTIVE)
                .build();

        MatchProject matchProject = MatchProject.builder()
                .match(savedMatch)
                .projectTemplate(template)
                .projectIdea(idea)
                .build();

        when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
        when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.of(backendUser));
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        when(projectGeneratorService.generateProject(frontendUser, backendUser, null))
                .thenReturn(generated);
        when(matchProjectRepository.save(any(MatchProject.class))).thenReturn(matchProject);

        // Act
        MatchStatusResponse response = matchService.findOrQueueMatch(frontendUserId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("MATCHED");
        assertThat(response.matchId()).isEqualTo(savedMatch.getId());
        assertThat(response.partnerName()).isEqualTo("Backend Dev");
        assertThat(response.partnerRole()).isEqualTo("BACKEND");
        assertThat(response.projectTitle()).isEqualTo("Todo App");

        // Verify both users' waitingSince was cleared
        verify(userRepository, times(2)).save(any(User.class));
        assertThat(frontendUser.getWaitingSince()).isNull();
        assertThat(backendUser.getWaitingSince()).isNull();

        // Verify participants were created
        verify(matchParticipantRepository, times(2)).save(any(MatchParticipant.class));
    }

    @Test
    void should_MatchFrontendWithBackend_When_RolesCorrect() {
        // Arrange - Frontend user looking for Backend
        ProjectTemplate template = TestDataBuilder.buildProjectTemplate("Chat App", "Real-time chat");
        ProjectGeneratorService.GeneratedProject generated =
                new ProjectGeneratorService.GeneratedProject(template, null, null, null);

        Match savedMatch = Match.builder().id(UUID.randomUUID()).status(MatchStatus.ACTIVE).build();
        MatchProject matchProject = MatchProject.builder()
                .match(savedMatch)
                .projectTemplate(template)
                .build();

        when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
        when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.of(backendUser)); // Found waiting backend user
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        when(projectGeneratorService.generateProject(frontendUser, backendUser, null))
                .thenReturn(generated);
        when(matchProjectRepository.save(any(MatchProject.class))).thenReturn(matchProject);

        // Act
        MatchStatusResponse response = matchService.findOrQueueMatch(frontendUserId);

        // Assert
        assertThat(response.status()).isEqualTo("MATCHED");
        assertThat(response.partnerRole()).isEqualTo("BACKEND");

        // Verify it looked for BACKEND role
        verify(userRepository).findOldestWaitingByRole("BACKEND", frontendUserId);
    }

    @Test
    void should_MatchBackendWithFrontend_When_BackendUserSearches() {
        // Arrange - Backend user looking for Frontend
        frontendUser.setWaitingSince(LocalDateTime.now().minusMinutes(3));

        ProjectTemplate template = TestDataBuilder.buildProjectTemplate("API", "REST API");
        ProjectGeneratorService.GeneratedProject generated =
                new ProjectGeneratorService.GeneratedProject(template, null, null, null);

        Match savedMatch = Match.builder().id(UUID.randomUUID()).status(MatchStatus.ACTIVE).build();
        MatchProject matchProject = MatchProject.builder()
                .match(savedMatch)
                .projectTemplate(template)
                .build();

        when(userRepository.findById(backendUserId)).thenReturn(Optional.of(backendUser));
        when(matchRepository.existsActiveMatchForUser(backendUserId, MatchStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findOldestWaitingByRole("FRONTEND", backendUserId))
                .thenReturn(Optional.of(frontendUser)); // Found waiting frontend user
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        when(projectGeneratorService.generateProject(frontendUser, backendUser, null))
                .thenReturn(generated);
        when(matchProjectRepository.save(any(MatchProject.class))).thenReturn(matchProject);

        // Act
        MatchStatusResponse response = matchService.findOrQueueMatch(backendUserId);

        // Assert
        assertThat(response.status()).isEqualTo("MATCHED");
        assertThat(response.partnerRole()).isEqualTo("FRONTEND");

        // Verify it looked for FRONTEND role
        verify(userRepository).findOldestWaitingByRole("FRONTEND", backendUserId);
    }

    @Test
    void should_JoinQueue_When_NoPartnerAvailable() {
        // Arrange
        when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
        when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.empty()); // No partner waiting

        // Act
        MatchStatusResponse response = matchService.findOrQueueMatch(frontendUserId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("WAITING");
        assertThat(response.waitingSince()).isNotNull();
        assertThat(response.queuePosition()).isEqualTo(1);
        assertThat(response.matchId()).isNull();
        assertThat(response.partnerName()).isNull();

        // Verify user was saved with waitingSince set
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getWaitingSince()).isNotNull();
    }

    @Test
    void should_NotUpdateWaitingSince_When_AlreadyWaiting() {
        // Arrange
        LocalDateTime existingWaitingSince = LocalDateTime.now().minusMinutes(10);
        frontendUser.setWaitingSince(existingWaitingSince); // Already waiting

        when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
        when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.empty());

        // Act
        MatchStatusResponse response = matchService.findOrQueueMatch(frontendUserId);

        // Assert
        assertThat(response.status()).isEqualTo("WAITING");
        assertThat(response.waitingSince()).isEqualTo(existingWaitingSince);

        // Verify user was NOT saved (already in queue)
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void should_FallbackToRandomTemplate_When_AIGenerationFails() {
        // Arrange
        ProjectTemplate fallbackTemplate = TestDataBuilder.buildProjectTemplate("Fallback", "Random template");

        Match savedMatch = Match.builder().id(UUID.randomUUID()).status(MatchStatus.ACTIVE).build();
        MatchProject matchProject = MatchProject.builder()
                .match(savedMatch)
                .projectTemplate(fallbackTemplate)
                .build();

        when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
        when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.of(backendUser));
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        when(projectGeneratorService.generateProject(frontendUser, backendUser, null))
                .thenReturn(null); // AI generation failed
        when(projectService.getRandomTemplate()).thenReturn(fallbackTemplate);
        when(matchProjectRepository.save(any(MatchProject.class))).thenReturn(matchProject);

        // Act
        MatchStatusResponse response = matchService.findOrQueueMatch(frontendUserId);

        // Assert
        assertThat(response.status()).isEqualTo("MATCHED");
        assertThat(response.projectTitle()).isEqualTo("Fallback");

        // Verify fallback was used
        verify(projectService).getRandomTemplate();
    }

    @Test
    void should_PassTopicToAI_When_TopicProvided() {
        // Arrange
        String topic = "Fintech";
        ProjectTemplate template = TestDataBuilder.buildProjectTemplate("Fintech App", "Banking app");
        ProjectGeneratorService.GeneratedProject generated =
                new ProjectGeneratorService.GeneratedProject(template, null, null, null);

        Match savedMatch = Match.builder().id(UUID.randomUUID()).status(MatchStatus.ACTIVE).build();
        MatchProject matchProject = MatchProject.builder()
                .match(savedMatch)
                .projectTemplate(template)
                .build();

        when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
        when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.of(backendUser));
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        when(projectGeneratorService.generateProject(frontendUser, backendUser, topic))
                .thenReturn(generated);
        when(matchProjectRepository.save(any(MatchProject.class))).thenReturn(matchProject);

        // Act
        MatchStatusResponse response = matchService.findOrQueueMatch(frontendUserId, topic);

        // Assert
        assertThat(response.status()).isEqualTo("MATCHED");

        // Verify topic was passed to AI
        verify(projectGeneratorService).generateProject(frontendUser, backendUser, topic);
    }

    @Test
    void should_CancelWaiting_When_UserInQueue() {
        // Arrange
        frontendUser.setWaitingSince(LocalDateTime.now().minusMinutes(5));
        when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));

        // Act
        matchService.cancelWaiting(frontendUserId);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getWaitingSince()).isNull();
    }

    @Test
    void should_DoNothing_When_CancelingUserNotInQueue() {
        // Arrange
        frontendUser.setWaitingSince(null); // Not waiting
        when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));

        // Act
        matchService.cancelWaiting(frontendUserId);

        // Assert
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void should_CompleteMatch_When_ValidRequest() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        Match match = Match.builder().id(matchId).status(MatchStatus.ACTIVE).build();

        MatchParticipant participant = MatchParticipant.builder()
                .match(match)
                .user(frontendUser)
                .build();

        MatchCompletionRequest request = new MatchCompletionRequest(null);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatch(match)).thenReturn(List.of(participant));

        // Act
        MatchCompletionResponse response = matchService.completeMatch(matchId, request, frontendUserId, null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.matchId()).isEqualTo(matchId);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.completedAt()).isNotNull();

        // Verify match status was updated
        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(matchCaptor.capture());
        assertThat(matchCaptor.getValue().getStatus()).isEqualTo(MatchStatus.COMPLETED);

        // Verify completion record was saved
        verify(matchCompletionRepository).save(any(MatchCompletion.class));
    }

    @Test
    void should_ThrowAccessDeniedException_When_NotParticipant() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        UUID nonParticipantId = UUID.randomUUID();
        Match match = Match.builder().id(matchId).status(MatchStatus.ACTIVE).build();

        MatchParticipant participant = MatchParticipant.builder()
                .match(match)
                .user(frontendUser) // Only frontendUser is participant
                .build();

        MatchCompletionRequest request = new MatchCompletionRequest(null);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatch(match)).thenReturn(List.of(participant));

        // Act & Assert
        assertThatThrownBy(() -> matchService.completeMatch(matchId, request, nonParticipantId, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining(nonParticipantId.toString())
                .hasMessageContaining("not authorized");

        // Verify match was NOT updated
        verify(matchRepository, never()).save(any(Match.class));
        verify(matchCompletionRepository, never()).save(any(MatchCompletion.class));
    }

    @Test
    void should_ThrowIllegalStateException_When_MatchNotActive() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        Match completedMatch = Match.builder().id(matchId).status(MatchStatus.COMPLETED).build();

        MatchCompletionRequest request = new MatchCompletionRequest(null);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(completedMatch));

        // Act & Assert
        assertThatThrownBy(() -> matchService.completeMatch(matchId, request, frontendUserId, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be completed")
                .hasMessageContaining("COMPLETED");

        // Verify no changes were made
        verify(matchParticipantRepository, never()).findByMatch(any());
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void should_GenerateAIReview_When_RepoUrlProvided() throws Exception {
        // Arrange
        UUID matchId = UUID.randomUUID();
        String repoUrl = "https://github.com/team/project";
        Match match = Match.builder().id(matchId).status(MatchStatus.ACTIVE).build();

        MatchParticipant participant = MatchParticipant.builder()
                .match(match)
                .user(frontendUser)
                .build();

        SprintReview review = SprintReview.builder()
                .match(match)
                .score(85)
                .aiFeedback("Great work!")
                .strengths("[]") // Empty JSON array for simplicity
                .missingElements("[]") // Empty JSON array for simplicity
                .build();

        MatchCompletionRequest request = new MatchCompletionRequest(repoUrl);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatch(match)).thenReturn(List.of(participant));
        when(sprintReviewService.generateReview(eq(match), eq(repoUrl), any())).thenReturn(Optional.of(review));

        // Act
        MatchCompletionResponse response = matchService.completeMatch(matchId, request, frontendUserId, null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.reviewScore()).isEqualTo(85);
        assertThat(response.reviewFeedback()).isEqualTo("Great work!");
        // JSON parsing returns empty lists for "[]"
        assertThat(response.reviewStrengths()).isEmpty();
        assertThat(response.reviewMissingElements()).isEmpty();

        verify(sprintReviewService).generateReview(eq(match), eq(repoUrl), any());
    }

    @Test
    void should_SkipAIReview_When_RepoUrlNull() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        Match match = Match.builder().id(matchId).status(MatchStatus.ACTIVE).build();

        MatchParticipant participant = MatchParticipant.builder()
                .match(match)
                .user(frontendUser)
                .build();

        MatchCompletionRequest request = new MatchCompletionRequest(null);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatch(match)).thenReturn(List.of(participant));

        // Act
        MatchCompletionResponse response = matchService.completeMatch(matchId, request, frontendUserId, null);

        // Assert
        assertThat(response.reviewScore()).isNull();
        assertThat(response.reviewFeedback()).isNull();
        assertThat(response.reviewStrengths()).isNull();
        assertThat(response.reviewMissingElements()).isNull();

        // Verify AI review was not called
        verify(sprintReviewService, never()).generateReview(any(), any(), any());
    }

    @Test
    void should_ContinueWithoutReview_When_AIGenerationFails() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        String repoUrl = "https://github.com/team/project";
        Match match = Match.builder().id(matchId).status(MatchStatus.ACTIVE).build();

        MatchParticipant participant = MatchParticipant.builder()
                .match(match)
                .user(frontendUser)
                .build();

        MatchCompletionRequest request = new MatchCompletionRequest(repoUrl);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatch(match)).thenReturn(List.of(participant));
        when(sprintReviewService.generateReview(eq(match), eq(repoUrl), any()))
                .thenThrow(new RuntimeException("AI service unavailable"));

        // Act
        MatchCompletionResponse response = matchService.completeMatch(matchId, request, frontendUserId, null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.reviewScore()).isNull(); // No review due to failure

        // Verify match was still completed
        verify(matchRepository).save(any(Match.class));
        verify(matchCompletionRepository).save(any(MatchCompletion.class));
    }

    @Test
    void should_BuildPartnerNameWithSurname_When_SurnameExists() {
        // Arrange - This is tested indirectly through findOrQueueMatch
        backendUser.setName("John");
        backendUser.setSurname("Doe");

        ProjectTemplate template = TestDataBuilder.buildProjectTemplate("App", "Test app");
        ProjectGeneratorService.GeneratedProject generated =
                new ProjectGeneratorService.GeneratedProject(template, null, null, null);

        Match savedMatch = Match.builder().id(UUID.randomUUID()).status(MatchStatus.ACTIVE).build();
        MatchProject matchProject = MatchProject.builder()
                .match(savedMatch)
                .projectTemplate(template)
                .build();

        when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
        when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.of(backendUser));
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        when(projectGeneratorService.generateProject(frontendUser, backendUser, null))
                .thenReturn(generated);
        when(matchProjectRepository.save(any(MatchProject.class))).thenReturn(matchProject);

        // Act
        MatchStatusResponse response = matchService.findOrQueueMatch(frontendUserId);

        // Assert
        assertThat(response.partnerName()).isEqualTo("John Doe");
    }

    @Test
    void should_BuildPartnerNameWithoutSurname_When_SurnameNull() {
        // Arrange
        backendUser.setName("John");
        backendUser.setSurname(null);

        ProjectTemplate template = TestDataBuilder.buildProjectTemplate("App", "Test app");
        ProjectGeneratorService.GeneratedProject generated =
                new ProjectGeneratorService.GeneratedProject(template, null, null, null);

        Match savedMatch = Match.builder().id(UUID.randomUUID()).status(MatchStatus.ACTIVE).build();
        MatchProject matchProject = MatchProject.builder()
                .match(savedMatch)
                .projectTemplate(template)
                .build();

        when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
        when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.of(backendUser));
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        when(projectGeneratorService.generateProject(frontendUser, backendUser, null))
                .thenReturn(generated);
        when(matchProjectRepository.save(any(MatchProject.class))).thenReturn(matchProject);

        // Act
        MatchStatusResponse response = matchService.findOrQueueMatch(frontendUserId);

        // Assert
        assertThat(response.partnerName()).isEqualTo("John");
    }

    @Test
    void should_ThrowResourceNotFoundException_When_MatchNotFoundForCompletion() {
        // Arrange
        UUID nonExistentMatchId = UUID.randomUUID();
        MatchCompletionRequest request = new MatchCompletionRequest(null);

        when(matchRepository.findById(nonExistentMatchId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> matchService.completeMatch(nonExistentMatchId, request, frontendUserId, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Match")
                .hasMessageContaining(nonExistentMatchId.toString());
    }
}
