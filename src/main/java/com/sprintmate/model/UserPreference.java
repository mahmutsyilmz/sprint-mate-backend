package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Stores user preferences for personalized project generation.
 *
 * Business Intent:
 * Captures user's preferred themes, difficulty level, and learning goals.
 * When two users are matched, the system intersects their preferences
 * to select the most appropriate archetype + theme combination.
 */
@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The user this preference belongs to (one-to-one).
     */
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Difficulty preference: 1=beginner, 2=intermediate, 3=advanced.
     */
    @Column(name = "difficulty_preference")
    private Integer difficultyPreference;

    /**
     * Comma-separated learning goals (e.g., "WebSocket,GraphQL,Docker").
     * Technologies/patterns the user wants to learn through projects.
     */
    @Column(name = "learning_goals", length = 500)
    private String learningGoals;

    /**
     * Preferred project themes (many-to-many with ProjectTheme).
     */
    @ManyToMany
    @JoinTable(
        name = "user_preferred_themes",
        joinColumns = @JoinColumn(name = "user_preference_id"),
        inverseJoinColumns = @JoinColumn(name = "theme_id")
    )
    @Builder.Default
    private Set<ProjectTheme> preferredThemes = new HashSet<>();
}
