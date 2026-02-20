package com.sprintmate.mapper;

import com.sprintmate.dto.MatchCompletionResponse;
import com.sprintmate.dto.MatchStatusResponse;
import com.sprintmate.model.Match;
import com.sprintmate.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for converting match-related entities to DTOs.
 *
 * Business Intent:
 * Centralizes match entity-to-DTO conversion logic.
 * Follows the mandatory DTO + Mapper pattern.
 */
@Component
public class MatchMapper {

    /**
     * Creates a MATCHED status response with full match details.
     */
    public MatchStatusResponse toMatchedResponse(Match match, String partnerName,
                                                  String partnerRole, String projectTitle,
                                                  String projectDescription) {
        return MatchStatusResponse.matched(
            match.getId(),
            match.getCommunicationLink(),
            partnerName,
            partnerRole,
            projectTitle,
            projectDescription
        );
    }

    /**
     * Creates a WAITING status response for users in queue.
     */
    public MatchStatusResponse toWaitingResponse(LocalDateTime waitingSince, int queuePosition) {
        return MatchStatusResponse.waiting(waitingSince, queuePosition);
    }

    /**
     * Creates a completion response without review.
     */
    public MatchCompletionResponse toCompletionResponse(UUID matchId, LocalDateTime completedAt,
                                                         String repoUrl) {
        return MatchCompletionResponse.of(matchId, completedAt, repoUrl);
    }

    /**
     * Creates a completion response with AI review data.
     */
    public MatchCompletionResponse toCompletionResponseWithReview(UUID matchId, LocalDateTime completedAt,
                                                                   String repoUrl, Integer reviewScore,
                                                                   String reviewFeedback,
                                                                   List<String> reviewStrengths,
                                                                   List<String> reviewMissingElements) {
        return MatchCompletionResponse.withReview(
            matchId, completedAt, repoUrl,
            reviewScore, reviewFeedback,
            reviewStrengths, reviewMissingElements
        );
    }

    /**
     * Builds the partner's display name.
     */
    public String buildPartnerName(User partner) {
        if (partner.getSurname() != null && !partner.getSurname().isEmpty()) {
            return partner.getName() + " " + partner.getSurname();
        }
        return partner.getName();
    }
}
