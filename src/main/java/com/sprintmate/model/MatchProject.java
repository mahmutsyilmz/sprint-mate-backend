package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a project instance assigned to a match.
 * Links a specific match with a project template, defining the work to be done.
 * Includes start and end dates for project timeline management.
 */
@Entity
@Table(name = "match_projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MatchProject {

    /**
     * Unique identifier for this match-project association.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the match this project belongs to.
     * One match can have multiple projects over time.
     */
    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    /**
     * Reference to the template this project is based on.
     * The template defines the requirements and structure.
     */
    @ManyToOne
    @JoinColumn(name = "project_template_id", nullable = false)
    private ProjectTemplate projectTemplate;

    /**
     * @deprecated Use archetype + theme instead. Kept for backward compatibility.
     */
    @Deprecated
    @ManyToOne
    @JoinColumn(name = "project_idea_id")
    private ProjectIdea projectIdea;

    /**
     * @deprecated Use archetype + theme instead. Kept for backward compatibility.
     */
    @Deprecated
    @ManyToOne
    @JoinColumn(name = "project_prompt_context_id")
    private ProjectPromptContext projectPromptContext;

    /**
     * The archetype used to generate this project (e.g., CRUD_APP, REAL_TIME_APP).
     */
    @ManyToOne
    @JoinColumn(name = "archetype_id")
    private ProjectArchetype archetype;

    /**
     * The theme used to generate this project (e.g., finance, health).
     */
    @ManyToOne
    @JoinColumn(name = "theme_id")
    private ProjectTheme theme;

    /**
     * The date when work on this project should begin.
     */
    @Column(nullable = false)
    private LocalDate startDate;

    /**
     * The target completion date for this project.
     */
    @Column(nullable = false)
    private LocalDate endDate;
}
