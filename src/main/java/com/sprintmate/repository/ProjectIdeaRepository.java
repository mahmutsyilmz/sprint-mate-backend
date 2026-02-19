package com.sprintmate.repository;

import com.sprintmate.model.ProjectIdea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ProjectIdea entity operations.
 */
@Repository
public interface ProjectIdeaRepository extends JpaRepository<ProjectIdea, UUID> {

    /**
     * Finds all active project ideas with pagination.
     */
    @Query("SELECT p FROM ProjectIdea p WHERE p.active = true")
    Page<ProjectIdea> findAllActivePaged(Pageable pageable);

    /**
     * Counts all active project ideas.
     */
    @Query("SELECT COUNT(p) FROM ProjectIdea p WHERE p.active = true")
    long countActive();

    /**
     * Finds ideas by category.
     */
    List<ProjectIdea> findByCategoryAndActiveTrue(String category);

    /**
     * Finds ideas by difficulty range.
     */
    @Query("SELECT p FROM ProjectIdea p WHERE p.active = true AND p.difficulty BETWEEN :min AND :max")
    List<ProjectIdea> findByDifficultyRange(@Param("min") int min, @Param("max") int max);
}
