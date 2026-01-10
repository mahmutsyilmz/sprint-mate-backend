package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents a participant in a match.
 * Links a user to a match with their specific role (FRONTEND or BACKEND).
 * Each match should have exactly one frontend and one backend participant.
 */
@Entity
@Table(name = "match_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchParticipant {

    /**
     * Unique identifier for this match participation.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the match.
     * Many participants can belong to different matches.
     */
    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    /**
     * Reference to the user participating in the match.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The role of the participant in this specific match.
     * Determines whether they work on frontend or backend tasks.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole participantRole;
}
