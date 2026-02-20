package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a user in the Sprint Mate platform.
 * Users can have multiple roles (Frontend, Backend) and participate in matches.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Required by @Builder, private for encapsulation
@Builder
public class User {

    /**
     * Unique identifier for the user.
     * Uses UUID for better scalability and security.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User's GitHub profile URL.
     * Used for identity verification and profile information.
     */
    private String githubUrl;

    /**
     * User's first name.
     */
    private String name;

    /**
     * User's last name.
     */
    @Column(name = "surname", nullable = true)
    private String surname;

    /**
     * User's selected role (FRONTEND or BACKEND).
     * Nullable until user explicitly selects a role after registration.
     * Determines which type of developer they will be matched with.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private RoleName role;

    /**
     * User's bio/title (e.g., "Full-stack developer", "React enthusiast").
     * Optional field for profile personalization.
     */
    @Column(name = "bio", length = 255)
    private String bio;

    /**
     * User's tech stack / skills (e.g., "Java", "React", "Docker").
     * Used for AI-driven project generation to create personalized project suggestions.
     * Stored in a separate table (user_skills) via @ElementCollection.
     */
    @ElementCollection
    @CollectionTable(name = "user_skills", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "skill")
    @Builder.Default
    private Set<String> skills = new HashSet<>();

    /**
     * Timestamp when user started waiting in the matching queue.
     * Null means user is not currently waiting for a match.
     * Used for FIFO queue ordering - oldest waiting user gets matched first.
     */
    @Column(name = "waiting_since")
    private LocalDateTime waitingSince;

    /**
     * User's project generation preferences (themes, difficulty, learning goals).
     * Optional - users without preferences get default behavior.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserPreference preference;
}
