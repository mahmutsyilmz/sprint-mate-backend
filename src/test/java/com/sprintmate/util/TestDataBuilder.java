package com.sprintmate.util;

import com.sprintmate.model.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Test data builder utility for creating test entities.
 * Provides convenient methods to create test objects with sensible defaults.
 *
 * Usage:
 * - User user = TestDataBuilder.buildUser(RoleName.FRONTEND);
 * - Match match = TestDataBuilder.buildMatch(MatchStatus.ACTIVE);
 */
public class TestDataBuilder {

    /**
     * Builds a User with the specified role.
     *
     * @param role The role to assign
     * @return A User entity with default test values
     */
    public static User buildUser(RoleName role) {
        return User.builder()
                .id(UUID.randomUUID())
                .githubUrl("https://github.com/testuser")
                .name("Test User")
                .role(role)
                .skills(new HashSet<>())
                .build();
    }

    /**
     * Builds a User with custom attributes.
     *
     * @param id     User ID
     * @param name   User name
     * @param role   User role
     * @param skills User skills
     * @return A User entity
     */
    public static User buildUser(UUID id, String name, RoleName role, Set<String> skills) {
        return User.builder()
                .id(id)
                .githubUrl("https://github.com/" + name.toLowerCase().replace(" ", ""))
                .name(name)
                .role(role)
                .skills(skills != null ? skills : new HashSet<>())
                .build();
    }

    /**
     * Builds a User with waiting status.
     *
     * @param role         User role
     * @param waitingSince Timestamp when user started waiting
     * @return A User entity in waiting state
     */
    public static User buildWaitingUser(RoleName role, LocalDateTime waitingSince) {
        User user = buildUser(role);
        user.setWaitingSince(waitingSince);
        return user;
    }

    /**
     * Builds a Match with the specified status.
     *
     * @param status Match status
     * @return A Match entity with default test values
     */
    public static Match buildMatch(MatchStatus status) {
        return Match.builder()
                .id(UUID.randomUUID())
                .status(status)
                .build();
    }

    /**
     * Builds a MatchParticipant linking a user to a match.
     *
     * @param match Match entity
     * @param user  User entity
     * @param role  Participant role
     * @return A MatchParticipant entity
     */
    public static MatchParticipant buildMatchParticipant(Match match, User user, ParticipantRole role) {
        return MatchParticipant.builder()
                .id(UUID.randomUUID())
                .match(match)
                .user(user)
                .participantRole(role)
                .build();
    }

    /**
     * Builds a ProjectTemplate.
     *
     * @param title       Template title
     * @param description Template description
     * @return A ProjectTemplate entity
     */
    public static ProjectTemplate buildProjectTemplate(String title, String description) {
        return ProjectTemplate.builder()
                .id(UUID.randomUUID())
                .title(title)
                .description(description)
                .build();
    }

    /**
     * Builds a ChatMessage.
     *
     * @param matchId    Match ID
     * @param senderId   Sender user ID
     * @param senderName Sender name
     * @param content    Message content
     * @return A ChatMessage entity
     */
    public static ChatMessage buildChatMessage(UUID matchId, UUID senderId, String senderName, String content) {
        return ChatMessage.builder()
                .id(UUID.randomUUID())
                .matchId(matchId)
                .senderId(senderId)
                .senderName(senderName)
                .content(content)
                .build();
    }

    /**
     * Builds a MatchCompletion.
     *
     * @param match   Match entity
     * @param repoUrl GitHub repository URL
     * @return A MatchCompletion entity
     */
    public static MatchCompletion buildMatchCompletion(Match match, String repoUrl) {
        return MatchCompletion.builder()
                .id(UUID.randomUUID())
                .match(match)
                .repoUrl(repoUrl)
                .build();
    }
}
