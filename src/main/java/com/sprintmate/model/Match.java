package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a match between developers.
 * A match connects frontend and backend developers for collaborative projects.
 * Matches have a lifecycle: CREATED → ACTIVE → COMPLETED
 */
@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Match {

    /**
     * Unique identifier for the match.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Current status of the match.
     * Tracks the lifecycle from creation to completion.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MatchStatus status = MatchStatus.CREATED;

    /**
     * Timestamp when the match was created.
     * Automatically set by the database on insert.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the match expires.
     * Can be set based on business logic or left null for indefinite matches.
     */
    private LocalDateTime expiresAt;

    /**
     * Link for communication between the matched developers (e.g., Discord, Slack, email).
     * Can be set by the system or users to facilitate collaboration.
     */
    private String communicationLink;
}
