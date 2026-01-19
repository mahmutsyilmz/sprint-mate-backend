package com.sprintmate.repository;

import com.sprintmate.model.ProjectPromptContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for ProjectPromptContext entity persistence operations.
 * Provides methods for fetching crisis scenarios for AI project generation.
 */
@Repository
public interface ProjectPromptContextRepository extends JpaRepository<ProjectPromptContext, UUID> {

    /**
     * Fetches all crisis contexts with pagination.
     * Used with random page selection for database-agnostic random row fetching.
     *
     * @param pageable Pagination info
     * @return Page of ProjectPromptContext
     */
    @Query("SELECT p FROM ProjectPromptContext p")
    Page<ProjectPromptContext> findAllPaged(Pageable pageable);
}
