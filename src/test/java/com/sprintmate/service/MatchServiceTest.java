package com.sprintmate.service;

import com.sprintmate.dto.MatchCompletionRequest;
import com.sprintmate.dto.MatchCompletionResponse;
import com.sprintmate.dto.MatchStatusResponse;
import com.sprintmate.exception.ActiveMatchExistsException;
import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.exception.RoleNotSelectedException;
import com.sprintmate.model.*;
import com.sprintmate.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchService.
 * Tests the core matching algorithm with FIFO queue in isolation using Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService Tests")
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

    @InjectMocks
    private MatchService matchService;

    private UUID frontendUserId;
    private UUID backendUserId;
    private User frontendUser;
    private User backendUser;
    private ProjectTemplate projectTemplate;
    private Match createdMatch;

    @BeforeEach
    void setUp() {
        frontendUserId = UUID.randomUUID();
        backendUserId = UUID.randomUUID();

        frontendUser = User.builder()
            .id(frontendUserId)
            .githubUrl("https://github.com/frontenddev")
            .name("Frontend")
            .surname("Developer")
            .role(RoleName.FRONTEND)
            .waitingSince(null)
            .build();

        backendUser = User.builder()
            .id(backendUserId)
            .githubUrl("https://github.com/backenddev")
            .name("Backend")
            .surname("Developer")
            .role(RoleName.BACKEND)
            .waitingSince(LocalDateTime.now().minusMinutes(5)) // Backend user is waiting
            .build();

        projectTemplate = ProjectTemplate.builder()
            .id(UUID.randomUUID())
            .title("E-Commerce MVP")
            .description("Build a full-stack e-commerce application")
            .build();

        createdMatch = Match.builder()
            .id(UUID.randomUUID())
            .status(MatchStatus.ACTIVE)
            .communicationLink("https://meet.google.com/mock-id")
            .build();
    }

    @Nested
    @DisplayName("Successful Match Tests")
    class SuccessfulMatchTests {

        @Test
        @DisplayName("should_CreateMatch_When_WaitingPartnerExists")
        void should_CreateMatch_When_WaitingPartnerExists() {
            // Arrange - Backend user is waiting in queue
            when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
            when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
            when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.of(backendUser));
            when(matchRepository.save(any(Match.class))).thenReturn(createdMatch);
            when(matchParticipantRepository.save(any(MatchParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(projectService.getRandomTemplate()).thenReturn(projectTemplate);
            when(matchProjectRepository.save(any(MatchProject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MatchStatusResponse result = matchService.findOrQueueMatch(frontendUserId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("MATCHED");
            assertThat(result.matchId()).isEqualTo(createdMatch.getId());
            assertThat(result.meetingUrl()).isEqualTo("https://meet.google.com/mock-id");
            assertThat(result.partnerName()).isEqualTo("Backend Developer");
            assertThat(result.partnerRole()).isEqualTo("BACKEND");
            assertThat(result.projectTitle()).isEqualTo("E-Commerce MVP");

            // Verify both users' waitingSince was cleared
            verify(userRepository, times(2)).save(any(User.class));
        }

        @Test
        @DisplayName("should_MatchWithOldestWaitingUser_FIFO")
        void should_MatchWithOldestWaitingUser_FIFO() {
            // Arrange
            when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
            when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
            when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.of(backendUser));
            when(matchRepository.save(any(Match.class))).thenReturn(createdMatch);
            when(matchParticipantRepository.save(any(MatchParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(projectService.getRandomTemplate()).thenReturn(projectTemplate);
            when(matchProjectRepository.save(any(MatchProject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MatchStatusResponse result = matchService.findOrQueueMatch(frontendUserId);

            // Assert - verify the query was called with correct parameters
            verify(userRepository).findOldestWaitingByRole("BACKEND", frontendUserId);
            assertThat(result.status()).isEqualTo("MATCHED");
        }

        @Test
        @DisplayName("should_CreateMatchParticipantsWithCorrectRoles")
        void should_CreateMatchParticipantsWithCorrectRoles() {
            // Arrange
            when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
            when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
            when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.of(backendUser));
            when(matchRepository.save(any(Match.class))).thenReturn(createdMatch);
            when(matchParticipantRepository.save(any(MatchParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(projectService.getRandomTemplate()).thenReturn(projectTemplate);
            when(matchProjectRepository.save(any(MatchProject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            matchService.findOrQueueMatch(frontendUserId);

            // Assert - Capture and verify participants
            ArgumentCaptor<MatchParticipant> participantCaptor = ArgumentCaptor.forClass(MatchParticipant.class);
            verify(matchParticipantRepository, times(2)).save(participantCaptor.capture());

            var participants = participantCaptor.getAllValues();
            assertThat(participants).hasSize(2);
            
            assertThat(participants)
                .extracting(MatchParticipant::getParticipantRole)
                .containsExactlyInAnyOrder(ParticipantRole.FRONTEND, ParticipantRole.BACKEND);
        }
    }

    @Nested
    @DisplayName("Queue (Waiting) Tests")
    class QueueTests {

        @Test
        @DisplayName("should_JoinQueue_When_NoPartnerWaiting")
        void should_JoinQueue_When_NoPartnerWaiting() {
            // Arrange - No waiting partners
            when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
            when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
            when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MatchStatusResponse result = matchService.findOrQueueMatch(frontendUserId);

            // Assert
            assertThat(result.status()).isEqualTo("WAITING");
            assertThat(result.matchId()).isNull();
            assertThat(result.queuePosition()).isEqualTo(1);
            assertThat(result.waitingSince()).isNotNull();

            // Verify user was added to queue
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getWaitingSince()).isNotNull();
        }

        @Test
        @DisplayName("should_NotUpdateWaitingSince_When_AlreadyInQueue")
        void should_NotUpdateWaitingSince_When_AlreadyInQueue() {
            // Arrange - User already waiting
            LocalDateTime originalWaitingSince = LocalDateTime.now().minusMinutes(10);
            frontendUser.setWaitingSince(originalWaitingSince);

            when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
            when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
            when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.empty());

            // Act
            MatchStatusResponse result = matchService.findOrQueueMatch(frontendUserId);

            // Assert - waitingSince should remain the same
            assertThat(result.status()).isEqualTo("WAITING");
            assertThat(result.waitingSince()).isEqualTo(originalWaitingSince);
            
            // Verify save was NOT called (waitingSince wasn't updated)
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Cancel Waiting Tests")
    class CancelWaitingTests {

        @Test
        @DisplayName("should_ClearWaitingSince_When_CancelWaiting")
        void should_ClearWaitingSince_When_CancelWaiting() {
            // Arrange
            frontendUser.setWaitingSince(LocalDateTime.now());
            when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            matchService.cancelWaiting(frontendUserId);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getWaitingSince()).isNull();
        }

        @Test
        @DisplayName("should_DoNothing_When_UserNotWaiting")
        void should_DoNothing_When_UserNotWaiting() {
            // Arrange - User not in queue
            frontendUser.setWaitingSince(null);
            when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));

            // Act
            matchService.cancelWaiting(frontendUserId);

            // Assert - save should not be called
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Active Match Exists Tests")
    class ActiveMatchExistsTests {

        @Test
        @DisplayName("should_ThrowException_When_UserHasActiveMatch")
        void should_ThrowException_When_UserHasActiveMatch() {
            // Arrange
            when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
            when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> matchService.findOrQueueMatch(frontendUserId))
                .isInstanceOf(ActiveMatchExistsException.class)
                .hasMessageContaining(frontendUserId.toString())
                .hasMessageContaining("already has an active match");

            // Verify no match was created
            verify(matchRepository, never()).save(any());
            verify(matchParticipantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("should_ThrowException_When_UserNotFound")
        void should_ThrowException_When_UserNotFound() {
            // Arrange
            UUID nonExistentUserId = UUID.randomUUID();
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> matchService.findOrQueueMatch(nonExistentUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should_ThrowException_When_UserHasNoRole")
        void should_ThrowException_When_UserHasNoRole() {
            // Arrange
            User userWithoutRole = User.builder()
                .id(frontendUserId)
                .githubUrl("https://github.com/newuser")
                .name("New User")
                .role(null)
                .build();

            when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(userWithoutRole));

            // Act & Assert
            assertThatThrownBy(() -> matchService.findOrQueueMatch(frontendUserId))
                .isInstanceOf(RoleNotSelectedException.class)
                .hasMessageContaining("must select a role");
        }

        @Test
        @DisplayName("should_HandlePartnerWithNoSurname")
        void should_HandlePartnerWithNoSurname() {
            // Arrange
            User partnerNoSurname = User.builder()
                .id(backendUserId)
                .githubUrl("https://github.com/backenddev")
                .name("SingleName")
                .surname(null)
                .role(RoleName.BACKEND)
                .waitingSince(LocalDateTime.now().minusMinutes(5))
                .build();

            when(userRepository.findById(frontendUserId)).thenReturn(Optional.of(frontendUser));
            when(matchRepository.existsActiveMatchForUser(frontendUserId, MatchStatus.ACTIVE)).thenReturn(false);
            when(userRepository.findOldestWaitingByRole("BACKEND", frontendUserId))
                .thenReturn(Optional.of(partnerNoSurname));
            when(matchRepository.save(any(Match.class))).thenReturn(createdMatch);
            when(matchParticipantRepository.save(any(MatchParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(projectService.getRandomTemplate()).thenReturn(projectTemplate);
            when(matchProjectRepository.save(any(MatchProject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MatchStatusResponse result = matchService.findOrQueueMatch(frontendUserId);

            // Assert - Partner name should be just the first name
            assertThat(result.partnerName()).isEqualTo("SingleName");
        }
    }

    @Nested
    @DisplayName("Complete Match Tests")
    class CompleteMatchTests {

        private UUID matchId;
        private Match activeMatch;
        private MatchParticipant frontendParticipant;
        private MatchParticipant backendParticipant;

        @BeforeEach
        void setUpCompleteMatchTests() {
            matchId = UUID.randomUUID();
            activeMatch = Match.builder()
                .id(matchId)
                .status(MatchStatus.ACTIVE)
                .communicationLink("https://meet.google.com/mock-id")
                .build();

            frontendParticipant = MatchParticipant.builder()
                .id(UUID.randomUUID())
                .match(activeMatch)
                .user(frontendUser)
                .participantRole(ParticipantRole.FRONTEND)
                .build();

            backendParticipant = MatchParticipant.builder()
                .id(UUID.randomUUID())
                .match(activeMatch)
                .user(backendUser)
                .participantRole(ParticipantRole.BACKEND)
                .build();
        }

        @Test
        @DisplayName("should_CompleteMatch_When_ValidParticipant")
        void should_CompleteMatch_When_ValidParticipant() {
            // Arrange
            String expectedRepoUrl = "https://github.com/team/project";
            MatchCompletionRequest request = new MatchCompletionRequest(expectedRepoUrl);

            when(matchRepository.findById(matchId)).thenReturn(Optional.of(activeMatch));
            when(matchParticipantRepository.findByMatch(activeMatch))
                .thenReturn(List.of(frontendParticipant, backendParticipant));
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(matchCompletionRepository.save(any(MatchCompletion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MatchCompletionResponse result = matchService.completeMatch(matchId, request, frontendUserId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.matchId()).isEqualTo(matchId);
            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.completedAt()).isNotNull();
            assertThat(result.repoUrl()).isEqualTo(expectedRepoUrl);

            // Verify match status was updated
            ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
            verify(matchRepository).save(matchCaptor.capture());
            assertThat(matchCaptor.getValue().getStatus()).isEqualTo(MatchStatus.COMPLETED);

            // Verify completion record was created with repo URL
            ArgumentCaptor<MatchCompletion> completionCaptor = ArgumentCaptor.forClass(MatchCompletion.class);
            verify(matchCompletionRepository).save(completionCaptor.capture());
            assertThat(completionCaptor.getValue().getRepoUrl()).isEqualTo(expectedRepoUrl);
        }

        @Test
        @DisplayName("should_CompleteMatch_WithoutRepoUrl")
        void should_CompleteMatch_WithoutRepoUrl() {
            // Arrange - null request
            when(matchRepository.findById(matchId)).thenReturn(Optional.of(activeMatch));
            when(matchParticipantRepository.findByMatch(activeMatch))
                .thenReturn(List.of(frontendParticipant, backendParticipant));
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(matchCompletionRepository.save(any(MatchCompletion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MatchCompletionResponse result = matchService.completeMatch(matchId, null, frontendUserId);

            // Assert
            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.repoUrl()).isNull();

            // Verify completion record was created without repo URL
            ArgumentCaptor<MatchCompletion> completionCaptor = ArgumentCaptor.forClass(MatchCompletion.class);
            verify(matchCompletionRepository).save(completionCaptor.capture());
            assertThat(completionCaptor.getValue().getRepoUrl()).isNull();
        }

        @Test
        @DisplayName("should_ThrowAccessDenied_When_UserNotParticipant")
        void should_ThrowAccessDenied_When_UserNotParticipant() {
            // Arrange
            UUID nonParticipantUserId = UUID.randomUUID();
            MatchCompletionRequest request = new MatchCompletionRequest("https://github.com/team/project");

            when(matchRepository.findById(matchId)).thenReturn(Optional.of(activeMatch));
            when(matchParticipantRepository.findByMatch(activeMatch))
                .thenReturn(List.of(frontendParticipant, backendParticipant));

            // Act & Assert
            assertThatThrownBy(() -> matchService.completeMatch(matchId, request, nonParticipantUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized to complete match");

            // Verify match was not updated
            verify(matchRepository, never()).save(any());
            verify(matchCompletionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_ThrowException_When_MatchAlreadyCompleted")
        void should_ThrowException_When_MatchAlreadyCompleted() {
            // Arrange
            Match completedMatch = Match.builder()
                .id(matchId)
                .status(MatchStatus.COMPLETED)
                .build();
            MatchCompletionRequest request = new MatchCompletionRequest("https://github.com/team/project");

            when(matchRepository.findById(matchId)).thenReturn(Optional.of(completedMatch));

            // Act & Assert
            assertThatThrownBy(() -> matchService.completeMatch(matchId, request, frontendUserId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be completed")
                .hasMessageContaining("COMPLETED");

            // Verify no updates were made
            verify(matchRepository, never()).save(any());
            verify(matchCompletionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_ThrowException_When_MatchNotFound")
        void should_ThrowException_When_MatchNotFound() {
            // Arrange
            UUID nonExistentMatchId = UUID.randomUUID();
            MatchCompletionRequest request = new MatchCompletionRequest("https://github.com/team/project");

            when(matchRepository.findById(nonExistentMatchId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> matchService.completeMatch(nonExistentMatchId, request, frontendUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Match not found");
        }

        @Test
        @DisplayName("should_AllowBothParticipants_ToCompleteMatch")
        void should_AllowBothParticipants_ToCompleteMatch() {
            // Arrange - Backend user completes
            MatchCompletionRequest request = new MatchCompletionRequest("https://github.com/team/project");

            when(matchRepository.findById(matchId)).thenReturn(Optional.of(activeMatch));
            when(matchParticipantRepository.findByMatch(activeMatch))
                .thenReturn(List.of(frontendParticipant, backendParticipant));
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(matchCompletionRepository.save(any(MatchCompletion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Act - Backend user attempts to complete
            MatchCompletionResponse result = matchService.completeMatch(matchId, request, backendUserId);

            // Assert
            assertThat(result.status()).isEqualTo("COMPLETED");
        }
    }
}
