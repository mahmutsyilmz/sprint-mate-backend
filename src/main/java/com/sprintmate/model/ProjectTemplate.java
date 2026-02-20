package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents a reusable project template.
 * Templates define the structure and requirements for projects that matched pairs will work on.
 * Admins can create multiple templates for different types of collaborative projects.
 */
@Entity
@Table(name = "project_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectTemplate {

    /**
     * Unique identifier for the project template.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The title of the project template.
     * Should be concise and descriptive (e.g., "E-commerce Dashboard").
     */
    @Column(nullable = false)
    private String title;

    /**
     * Detailed description of the project requirements and expectations.
     * Includes technical specifications, features to implement, and acceptance criteria.
     */
    @Column(length = 2000, columnDefinition = "NVARCHAR(2000)")
    private String description;
}
