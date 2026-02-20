package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a fun, portfolio-worthy project idea for sprint generation.
 *
 * Business Intent:
 * Stores exciting project concepts that developers would actually want to build.
 * These are NOT crisis scenarios - they are creative, engaging mini-projects
 * that can be completed in 1 week and shown off in portfolios.
 *
 * The AI uses these as inspiration to generate specific tasks based on
 * the matched developers' skills.
 */
@Entity
@Table(name = "project_ideas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectIdea {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Project category (e.g., "Social", "Productivity", "Gaming", "E-Commerce").
     */
    @Column(nullable = false)
    private String category;

    /**
     * Short, catchy project name (e.g., "Mini Twitter Clone", "Recipe Sharing App").
     */
    @Column(nullable = false)
    private String name;

    /**
     * One-liner pitch that makes developers excited.
     * (e.g., "Build the next viral social app with real-time features!")
     */
    @Column(nullable = false, length = 500)
    private String pitch;

    /**
     * Core concept description - what makes this project special.
     */
    @Column(name = "core_concept", nullable = false, length = 1000)
    private String coreConcept;

    /**
     * Key features that MUST be implemented (3-5 features).
     * Stored as comma-separated values.
     */
    @Column(name = "key_features", nullable = false, length = 1000)
    private String keyFeatures;

    /**
     * Bonus/stretch goals for ambitious teams.
     * Stored as comma-separated values.
     */
    @Column(name = "bonus_features", length = 500)
    private String bonusFeatures;

    /**
     * Example use case or user story.
     */
    @Column(name = "example_use_case", length = 500)
    private String exampleUseCase;

    /**
     * Why this project is great for portfolios.
     */
    @Column(name = "portfolio_value", length = 500)
    private String portfolioValue;

    /**
     * Difficulty level (1-5): 1=Beginner, 3=Intermediate, 5=Advanced
     */
    @Column(nullable = false)
    private Integer difficulty;

    /**
     * Tags for filtering (e.g., "real-time,social,crud").
     */
    @Column(length = 200)
    private String tags;

    /**
     * Whether this idea is active and can be used.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
