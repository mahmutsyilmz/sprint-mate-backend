package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Join table representing the many-to-many relationship between users and roles.
 * A user can have multiple roles (e.g., both FRONTEND and BACKEND).
 */
@Entity
@Table(name = "user_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

    /**
     * Unique identifier for this user-role association.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the user.
     * Many user-role associations can point to one user.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Reference to the role.
     * Many user-role associations can point to one role.
     */
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}
