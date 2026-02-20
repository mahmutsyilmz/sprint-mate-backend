package com.sprintmate.mapper;

import com.sprintmate.dto.ChatMessageResponse;
import com.sprintmate.model.ChatMessage;
import com.sprintmate.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ChatMapper.
 * Tests entity-to-DTO conversion logic for chat messages.
 */
class ChatMapperTest {

    private ChatMapper chatMapper;

    @BeforeEach
    void setUp() {
        chatMapper = new ChatMapper();
    }

    @Test
    void should_MapMessageToResponse_When_AllFieldsPresent() {
        // Arrange
        UUID messageId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ChatMessage message = ChatMessage.builder()
                .id(messageId)
                .matchId(matchId)
                .senderId(senderId)
                .senderName("John Doe")
                .content("Hello, how's the project going?")
                .build();

        // Manually set createdAt (since @CreationTimestamp won't work in unit test)
        message.setCreatedAt(now);

        // Act
        ChatMessageResponse response = chatMapper.toResponse(message);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(messageId);
        assertThat(response.matchId()).isEqualTo(matchId);
        assertThat(response.senderId()).isEqualTo(senderId);
        assertThat(response.senderName()).isEqualTo("John Doe");
        assertThat(response.content()).isEqualTo("Hello, how's the project going?");
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void should_HandleLongContent_When_MappingMessage() {
        // Arrange
        String longContent = "This is a very long message content that might exceed normal length. ".repeat(20);
        ChatMessage message = TestDataBuilder.buildChatMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Sender",
                longContent
        );
        message.setCreatedAt(LocalDateTime.now());

        // Act
        ChatMessageResponse response = chatMapper.toResponse(message);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo(longContent);
        assertThat(response.content().length()).isGreaterThan(100);
    }

    @Test
    void should_MapEmptyListToEmptyResponse_When_NoMessages() {
        // Arrange
        List<ChatMessage> messages = List.of();

        // Act
        List<ChatMessageResponse> responses = chatMapper.toResponseList(messages);

        // Assert
        assertThat(responses).isNotNull();
        assertThat(responses).isEmpty();
    }

    @Test
    void should_MapMultipleMessagesToResponses_When_ListProvided() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        LocalDateTime time1 = LocalDateTime.now().minusMinutes(10);
        LocalDateTime time2 = LocalDateTime.now().minusMinutes(5);
        LocalDateTime time3 = LocalDateTime.now();

        ChatMessage msg1 = TestDataBuilder.buildChatMessage(matchId, UUID.randomUUID(), "Alice", "Hi!");
        msg1.setCreatedAt(time1);

        ChatMessage msg2 = TestDataBuilder.buildChatMessage(matchId, UUID.randomUUID(), "Bob", "Hello Alice");
        msg2.setCreatedAt(time2);

        ChatMessage msg3 = TestDataBuilder.buildChatMessage(matchId, UUID.randomUUID(), "Alice", "How are you?");
        msg3.setCreatedAt(time3);

        List<ChatMessage> messages = List.of(msg1, msg2, msg3);

        // Act
        List<ChatMessageResponse> responses = chatMapper.toResponseList(messages);

        // Assert
        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).senderName()).isEqualTo("Alice");
        assertThat(responses.get(0).content()).isEqualTo("Hi!");
        assertThat(responses.get(1).senderName()).isEqualTo("Bob");
        assertThat(responses.get(2).content()).isEqualTo("How are you?");
    }

    @Test
    void should_PreserveDataIntegrity_When_MappingList() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        UUID senderId1 = UUID.randomUUID();
        UUID senderId2 = UUID.randomUUID();
        LocalDateTime time1 = LocalDateTime.now().minusHours(1);
        LocalDateTime time2 = LocalDateTime.now();

        ChatMessage msg1 = TestDataBuilder.buildChatMessage(matchId, senderId1, "User1", "First message");
        msg1.setCreatedAt(time1);

        ChatMessage msg2 = TestDataBuilder.buildChatMessage(matchId, senderId2, "User2", "Second message");
        msg2.setCreatedAt(time2);

        List<ChatMessage> messages = List.of(msg1, msg2);

        // Act
        List<ChatMessageResponse> responses = chatMapper.toResponseList(messages);

        // Assert
        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).matchId()).isEqualTo(matchId);
        assertThat(responses.get(0).senderId()).isEqualTo(senderId1);
        assertThat(responses.get(0).senderName()).isEqualTo("User1");
        assertThat(responses.get(0).content()).isEqualTo("First message");
        assertThat(responses.get(0).createdAt()).isEqualTo(time1);

        assertThat(responses.get(1).senderId()).isEqualTo(senderId2);
        assertThat(responses.get(1).senderName()).isEqualTo("User2");
        assertThat(responses.get(1).content()).isEqualTo("Second message");
        assertThat(responses.get(1).createdAt()).isEqualTo(time2);
    }

    @Test
    void should_MapSingleElementList_When_OneMessageProvided() {
        // Arrange
        ChatMessage message = TestDataBuilder.buildChatMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Solo User",
                "Single message in conversation"
        );
        message.setCreatedAt(LocalDateTime.now());

        List<ChatMessage> messages = List.of(message);

        // Act
        List<ChatMessageResponse> responses = chatMapper.toResponseList(messages);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).senderName()).isEqualTo("Solo User");
        assertThat(responses.get(0).content()).isEqualTo("Single message in conversation");
    }
}
