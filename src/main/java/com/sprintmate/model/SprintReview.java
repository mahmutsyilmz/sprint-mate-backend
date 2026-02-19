package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an AI-generated review of a completed sprint.
 *
 * Business Intent:
 * Stores the AI evaluation of a user's sprint submission by comparing
 * their GitHub README against the original crisis scenario requirements.
 * Provides feedback, scores, strengths, and missing elements.
 */
@Entity
@Table(name = "sprint_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SprintReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the match this review belongs to.
     */
    @OneToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    /**
     * The GitHub repository URL submitted for review.
     */
    @Column(name = "repo_url", nullable = false)
    private String repoUrl;

    /**
     * AI-generated score (0-100) based on how well the submission
     * addresses the crisis scenario requirements.
     */
    @Column(nullable = false)
    private Integer score;

    /**
     * Constructive AI-generated feedback about the submission.
     */
    @Column(name = "ai_feedback", length = 4000)
    private String aiFeedback;

    /**
     * JSON array of identified strengths in the submission.
     */
    @Column(length = 2000)
    private String strengths;

    /**
     * JSON array of missing or incomplete elements.
     */
    @Column(name = "missing_elements", length = 2000)
    private String missingElements;

    /**
     * The README content fetched from GitHub for analysis.
     */
    @Column(name = "readme_content", length = 10000)
    private String readmeContent;

    /**
     * Timestamp when this review was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
