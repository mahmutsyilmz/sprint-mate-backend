package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a role type in the system.
 * This is a lookup/reference table with predefined role types.
 * Uses Long ID as it's a small, fixed set of reference data.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    /**
     * Unique identifier for the role.
     * Uses Long ID as this is a small lookup table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the role.
     * Defines the type of developer (FRONTEND or BACKEND).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleName roleName;
}
