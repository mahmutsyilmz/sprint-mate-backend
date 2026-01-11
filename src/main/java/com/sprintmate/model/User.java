package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;

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
@AllArgsConstructor
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
}
