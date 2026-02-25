package com.sprintmate.mapper;

import com.sprintmate.dto.MatchCompletionResponse;
import com.sprintmate.dto.MatchStatusResponse;
import com.sprintmate.model.Match;
import com.sprintmate.model.MatchStatus;
import com.sprintmate.model.RoleName;
import com.sprintmate.model.User;
import com.sprintmate.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MatchMapper.
 * Tests entity-to-DTO conversion logic for match-related responses.
 */
class MatchMapperTest {

    private MatchMapper matchMapper;

    @BeforeEach
    void setUp() {
        matchMapper = new MatchMapper();
    }

    @Test
    void should_MapToMatchedResponse_When_MatchProvided() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        Match match = Match.builder()
                .id(matchId)
                .status(MatchStatus.ACTIVE)
                .communicationLink("https://meet.google.com/abc")
                .build();

        // Act
        MatchStatusResponse response = matchMapper.toMatchedResponse(
                match, "John Doe", "BACKEND", "Todo App", "Build a task manager");

        // Assert
        assertThat(response.status()).isEqualTo("MATCHED");
        assertThat(response.matchId()).isEqualTo(matchId);
        assertThat(response.meetingUrl()).isEqualTo("https://meet.google.com/abc");
        assertThat(response.partnerName()).isEqualTo("John Doe");
        assertThat(response.partnerRole()).isEqualTo("BACKEND");
        assertThat(response.projectTitle()).isEqualTo("Todo App");
        assertThat(response.projectDescription()).isEqualTo("Build a task manager");
        assertThat(response.waitingSince()).isNull();
        assertThat(response.queuePosition()).isNull();
    }

    @Test
    void should_MapToWaitingResponse_When_WaitingSinceProvided() {
        // Arrange
        LocalDateTime waitingSince = LocalDateTime.now().minusMinutes(5);
        int queuePosition = 3;

        // Act
        MatchStatusResponse response = matchMapper.toWaitingResponse(waitingSince, queuePosition);

        // Assert
        assertThat(response.status()).isEqualTo("WAITING");
        assertThat(response.waitingSince()).isEqualTo(waitingSince);
        assertThat(response.queuePosition()).isEqualTo(3);
        assertThat(response.matchId()).isNull();
        assertThat(response.partnerName()).isNull();
        assertThat(response.partnerRole()).isNull();
    }

    @Test
    void should_MapToCompletionResponse_When_NoReviewProvided() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        LocalDateTime completedAt = LocalDateTime.now();
        String repoUrl = "https://github.com/team/project";

        // Act
        MatchCompletionResponse response = matchMapper.toCompletionResponse(matchId, completedAt, repoUrl);

        // Assert
        assertThat(response.matchId()).isEqualTo(matchId);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.completedAt()).isEqualTo(completedAt);
        assertThat(response.repoUrl()).isEqualTo(repoUrl);
        assertThat(response.reviewScore()).isNull();
        assertThat(response.reviewFeedback()).isNull();
        assertThat(response.reviewStrengths()).isNull();
        assertThat(response.reviewMissingElements()).isNull();
    }

    @Test
    void should_MapToCompletionResponseWithReview_When_ReviewProvided() {
        // Arrange
        UUID matchId = UUID.randomUUID();
        LocalDateTime completedAt = LocalDateTime.now();
        String repoUrl = "https://github.com/team/project";
        List<String> strengths = List.of("Clean code", "Good tests");
        List<String> missing = List.of("Documentation");

        // Act
        MatchCompletionResponse response = matchMapper.toCompletionResponseWithReview(
                matchId, completedAt, repoUrl, 85, "Great work!", strengths, missing);

        // Assert
        assertThat(response.matchId()).isEqualTo(matchId);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.repoUrl()).isEqualTo(repoUrl);
        assertThat(response.reviewScore()).isEqualTo(85);
        assertThat(response.reviewFeedback()).isEqualTo("Great work!");
        assertThat(response.reviewStrengths()).containsExactly("Clean code", "Good tests");
        assertThat(response.reviewMissingElements()).containsExactly("Documentation");
    }

    @Test
    void should_BuildPartnerNameWithSurname_When_SurnameExists() {
        // Arrange
        User partner = TestDataBuilder.buildUser(RoleName.BACKEND);
        partner.setName("Jane");
        partner.setSurname("Smith");

        // Act
        String name = matchMapper.buildPartnerName(partner);

        // Assert
        assertThat(name).isEqualTo("Jane Smith");
    }

    @Test
    void should_BuildPartnerNameWithoutSurname_When_SurnameNull() {
        // Arrange
        User partner = TestDataBuilder.buildUser(RoleName.BACKEND);
        partner.setName("Jane");
        partner.setSurname(null);

        // Act
        String name = matchMapper.buildPartnerName(partner);

        // Assert
        assertThat(name).isEqualTo("Jane");
    }

    @Test
    void should_BuildPartnerNameWithoutSurname_When_SurnameEmpty() {
        // Arrange
        User partner = TestDataBuilder.buildUser(RoleName.BACKEND);
        partner.setName("Jane");
        partner.setSurname("");

        // Act
        String name = matchMapper.buildPartnerName(partner);

        // Assert
        assertThat(name).isEqualTo("Jane");
    }
}
