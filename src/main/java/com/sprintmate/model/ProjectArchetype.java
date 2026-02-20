package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an abstract project structure pattern (e.g., CRUD_APP, REAL_TIME_APP).
 *
 * Business Intent:
 * Instead of hardcoded project ideas, archetypes define structural templates
 * that can be combined with any theme to generate unique projects.
 * The AI uses archetype patterns + theme context + user skills to create
 * personalized project suggestions.
 */
@Entity
@Table(name = "project_archetypes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectArchetype {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique code identifier (e.g., "CRUD_APP", "REAL_TIME_APP").
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Human-readable name (e.g., "CRUD Application").
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * Describes what this archetype structurally involves.
     */
    @Column(name = "structure_description", nullable = false, length = 500)
    private String structureDescription;

    /**
     * Comma-separated component patterns (e.g., "CRUD,REST,Pagination,Search").
     */
    @Column(name = "component_patterns", length = 500)
    private String componentPatterns;

    /**
     * Comma-separated API patterns (e.g., "REST,WebSocket").
     */
    @Column(name = "api_patterns", length = 300)
    private String apiPatterns;

    /**
     * Minimum complexity level this archetype supports (1-5).
     */
    @Column(name = "min_complexity", nullable = false)
    private Integer minComplexity;

    /**
     * Maximum complexity level this archetype supports (1-5).
     */
    @Column(name = "max_complexity", nullable = false)
    private Integer maxComplexity;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
