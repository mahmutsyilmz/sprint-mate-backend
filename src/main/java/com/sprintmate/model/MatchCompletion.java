package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents the completion record of a match.
 * Stores final information about the completed match, including the repository URL.
 * One-to-one relationship with Match - each match can have at most one completion record.
 */
@Entity
@Table(name = "match_completions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MatchCompletion {

    /**
     * Unique identifier for the match completion record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the completed match.
     * One-to-one relationship: each match has exactly one completion record.
     */
    @OneToOne
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    /**
     * Timestamp when the match was marked as completed.
     * Automatically set by the database on insert.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime completedAt;

    /**
     * URL to the GitHub/GitLab repository containing the project deliverable.
     * Optional - some matches may be completed without a repository.
     */
    private String repoUrl;
}
