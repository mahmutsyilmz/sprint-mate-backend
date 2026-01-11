package com.sprintmate.repository;

import com.sprintmate.model.ProjectTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for ProjectTemplate entity persistence operations.
 * Provides standard CRUD operations via JpaRepository for managing project templates.
 */
@Repository
public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplate, UUID> {
}
