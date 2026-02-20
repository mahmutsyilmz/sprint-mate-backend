package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents a domain/theme for project generation (e.g., finance, health, gaming).
 *
 * Business Intent:
 * Themes provide domain context that can be combined with any archetype
 * to produce unique project ideas. Users can set preferred themes in their
 * profile to personalize AI-generated project suggestions.
 */
@Entity
@Table(name = "project_themes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectTheme {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique code identifier (e.g., "finance", "health", "gaming").
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Human-readable name (e.g., "Finance", "Health & Fitness").
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * Domain context description for the AI prompt.
     * (e.g., "Financial data, budgets, transactions, investments")
     */
    @Column(name = "domain_context", length = 500)
    private String domainContext;

    /**
     * Comma-separated example domain entities for the AI prompt.
     * (e.g., "budget,transaction,portfolio,account")
     */
    @Column(name = "example_entities", length = 300)
    private String exampleEntities;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
