package com.sprintmate.service;

import com.sprintmate.dto.ChatMessageRequest;
import com.sprintmate.dto.ChatMessageResponse;
import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.mapper.ChatMapper;
import com.sprintmate.model.ChatMessage;
import com.sprintmate.model.RoleName;
import com.sprintmate.model.User;
import com.sprintmate.repository.ChatMessageRepository;
import com.sprintmate.repository.MatchParticipantRepository;
import com.sprintmate.repository.UserRepository;
import com.sprintmate.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService.
 * Tests chat message handling and access control logic.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMapper chatMapper;

    @InjectMocks
    private ChatService chatService;

    private UUID matchId;
    private UUID senderId;
    private User sender;
    private ChatMessageRequest request;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        senderId = UUID.randomUUID();

        sender = TestDataBuilder.buildUser(RoleName.FRONTEND);
        sender.setId(senderId);
        sender.setName("John");
        sender.setSurname("Doe");

        request = new ChatMessageRequest(matchId, "Hello, let's start the project!");
    }

    @Test
    void should_SaveMessage_When_ValidParticipant() {
        // Arrange
        ChatMessage savedMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .matchId(matchId)
                .senderId(senderId)
                .senderName("John Doe")
                .content("Hello, let's start the project!")
                .build();
        savedMessage.setCreatedAt(LocalDateTime.now());

        ChatMessageResponse expectedResponse = new ChatMessageResponse(
                savedMessage.getId(),
                matchId,
                senderId,
                "John Doe",
                "Hello, let's start the project!",
                savedMessage.getCreatedAt()
        );

        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, senderId)).thenReturn(true);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
        when(chatMapper.toResponse(savedMessage)).thenReturn(expectedResponse);

        // Act
        ChatMessageResponse response = chatService.saveMessage(request, senderId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.matchId()).isEqualTo(matchId);
        assertThat(response.senderId()).isEqualTo(senderId);
        assertThat(response.content()).isEqualTo("Hello, let's start the project!");
        assertThat(response.senderName()).isEqualTo("John Doe");

        // Verify message was saved
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());

        ChatMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getMatchId()).isEqualTo(matchId);
        assertThat(capturedMessage.getSenderId()).isEqualTo(senderId);
        assertThat(capturedMessage.getSenderName()).isEqualTo("John Doe");
        assertThat(capturedMessage.getContent()).isEqualTo("Hello, let's start the project!");
    }

    @Test
    void should_ThrowAccessDeniedException_When_NotParticipant() {
        // Arrange
        UUID nonParticipantId = UUID.randomUUID();
        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, nonParticipantId))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> chatService.saveMessage(request, nonParticipantId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not a participant");

        // Verify message was NOT saved
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void should_ThrowResourceNotFoundException_When_UserNotFound() {
        // Arrange
        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, senderId)).thenReturn(true);
        when(userRepository.findById(senderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> chatService.saveMessage(request, senderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(senderId.toString());

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void should_BuildSenderNameWithSurname_When_SurnameExists() {
        // Arrange
        sender.setName("Alice");
        sender.setSurname("Smith");

        ChatMessage savedMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .matchId(matchId)
                .senderId(senderId)
                .senderName("Alice Smith")
                .content("Test message")
                .build();

        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, senderId)).thenReturn(true);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
        when(chatMapper.toResponse(any())).thenReturn(mock(ChatMessageResponse.class));

        // Act
        chatService.saveMessage(request, senderId);

        // Assert
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getSenderName()).isEqualTo("Alice Smith");
    }

    @Test
    void should_BuildSenderNameWithoutSurname_When_SurnameNull() {
        // Arrange
        sender.setName("Bob");
        sender.setSurname(null);

        ChatMessage savedMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .matchId(matchId)
                .senderId(senderId)
                .senderName("Bob")
                .content("Test message")
                .build();

        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, senderId)).thenReturn(true);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
        when(chatMapper.toResponse(any())).thenReturn(mock(ChatMessageResponse.class));

        // Act
        chatService.saveMessage(request, senderId);

        // Assert
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getSenderName()).isEqualTo("Bob");
    }

    @Test
    void should_GetChatHistory_When_ValidParticipant() {
        // Arrange
        UUID userId = UUID.randomUUID();
        int limit = 50;

        // Use ArrayList (mutable) instead of List.of() because service calls Collections.reverse()
        List<ChatMessage> messages = new java.util.ArrayList<>(List.of(
                TestDataBuilder.buildChatMessage(matchId, senderId, "User1", "Message 1"),
                TestDataBuilder.buildChatMessage(matchId, senderId, "User2", "Message 2")
        ));

        List<ChatMessageResponse> expectedResponses = List.of(
                mock(ChatMessageResponse.class),
                mock(ChatMessageResponse.class)
        );

        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(true);
        when(chatMessageRepository.findByMatchIdOrderByCreatedAtDesc(eq(matchId), any(PageRequest.class)))
                .thenReturn(messages);
        when(chatMapper.toResponseList(any())).thenReturn(expectedResponses);

        // Act
        List<ChatMessageResponse> responses = chatService.getChatHistory(matchId, userId, limit);

        // Assert
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);

        verify(matchParticipantRepository).existsByMatchIdAndUserId(matchId, userId);
        verify(chatMessageRepository).findByMatchIdOrderByCreatedAtDesc(eq(matchId), eq(PageRequest.of(0, limit)));
    }

    @Test
    void should_ThrowAccessDeniedException_When_NonParticipantRequestsHistory() {
        // Arrange
        UUID nonParticipantId = UUID.randomUUID();
        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, nonParticipantId))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> chatService.getChatHistory(matchId, nonParticipantId, 50))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not a participant");

        verify(chatMessageRepository, never()).findByMatchIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void should_ReturnChronologicalHistory_When_Retrieving() {
        // Arrange - Validates Bug #16 fix (messages should be ordered chronologically)
        UUID userId = UUID.randomUUID();

        ChatMessage msg1 = TestDataBuilder.buildChatMessage(matchId, senderId, "User", "First");
        msg1.setCreatedAt(LocalDateTime.now().minusMinutes(10));

        ChatMessage msg2 = TestDataBuilder.buildChatMessage(matchId, senderId, "User", "Second");
        msg2.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        ChatMessage msg3 = TestDataBuilder.buildChatMessage(matchId, senderId, "User", "Third");
        msg3.setCreatedAt(LocalDateTime.now());

        // Repository returns in DESC order (newest first) - use ArrayList for mutability
        List<ChatMessage> messagesFromDb = new java.util.ArrayList<>(List.of(msg3, msg2, msg1));

        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(true);
        when(chatMessageRepository.findByMatchIdOrderByCreatedAtDesc(eq(matchId), any(PageRequest.class)))
                .thenReturn(messagesFromDb);
        when(chatMapper.toResponseList(any())).thenAnswer(invocation -> {
            List<ChatMessage> input = invocation.getArgument(0);
            // Verify the list passed to mapper is in chronological order (ASC)
            assertThat(input.get(0).getContent()).isEqualTo("First");
            assertThat(input.get(1).getContent()).isEqualTo("Second");
            assertThat(input.get(2).getContent()).isEqualTo("Third");
            return Collections.emptyList();
        });

        // Act
        chatService.getChatHistory(matchId, userId, 100);

        // Assert verified in doAnswer above
        verify(chatMapper).toResponseList(any());
    }

    @Test
    void should_UseDefaultLimit_When_LimitNotSpecified() {
        // Arrange
        UUID userId = UUID.randomUUID();

        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(true);
        when(chatMessageRepository.findByMatchIdOrderByCreatedAtDesc(eq(matchId), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());
        when(chatMapper.toResponseList(any())).thenReturn(Collections.emptyList());

        // Act
        chatService.getChatHistory(matchId, userId);

        // Assert - should use default limit of 100
        verify(chatMessageRepository).findByMatchIdOrderByCreatedAtDesc(eq(matchId), eq(PageRequest.of(0, 100)));
    }

    @Test
    void should_UseProvidedLimit_When_LimitSpecified() {
        // Arrange
        UUID userId = UUID.randomUUID();
        int customLimit = 25;

        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(true);
        when(chatMessageRepository.findByMatchIdOrderByCreatedAtDesc(eq(matchId), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());
        when(chatMapper.toResponseList(any())).thenReturn(Collections.emptyList());

        // Act
        chatService.getChatHistory(matchId, userId, customLimit);

        // Assert
        verify(chatMessageRepository).findByMatchIdOrderByCreatedAtDesc(eq(matchId), eq(PageRequest.of(0, customLimit)));
    }

    @Test
    void should_ReturnEmpty_When_NoMessages() {
        // Arrange
        UUID userId = UUID.randomUUID();

        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(true);
        when(chatMessageRepository.findByMatchIdOrderByCreatedAtDesc(eq(matchId), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());
        when(chatMapper.toResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

        // Act
        List<ChatMessageResponse> responses = chatService.getChatHistory(matchId, userId, 100);

        // Assert
        assertThat(responses).isEmpty();
    }

    @Test
    void should_ReturnTrue_When_CanAccessChatAsParticipant() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(true);

        // Act
        boolean canAccess = chatService.canAccessChat(matchId, userId);

        // Assert
        assertThat(canAccess).isTrue();
        verify(matchParticipantRepository).existsByMatchIdAndUserId(matchId, userId);
    }

    @Test
    void should_ReturnFalse_When_CannotAccessChatAsNonParticipant() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(matchParticipantRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(false);

        // Act
        boolean canAccess = chatService.canAccessChat(matchId, userId);

        // Assert
        assertThat(canAccess).isFalse();
        verify(matchParticipantRepository).existsByMatchIdAndUserId(matchId, userId);
    }
}
