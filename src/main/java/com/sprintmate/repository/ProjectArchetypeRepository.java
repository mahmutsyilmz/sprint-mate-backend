package com.sprintmate.repository;

import com.sprintmate.model.ProjectArchetype;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ProjectArchetype entity operations.
 */
@Repository
public interface ProjectArchetypeRepository extends JpaRepository<ProjectArchetype, UUID> {

    List<ProjectArchetype> findByActiveTrue();

    Optional<ProjectArchetype> findByCode(String code);

    /**
     * Finds active archetypes matching a given complexity level.
     */
    @Query("SELECT pa FROM ProjectArchetype pa WHERE pa.active = true " +
           "AND pa.minComplexity <= :level AND pa.maxComplexity >= :level")
    List<ProjectArchetype> findByComplexityLevel(@Param("level") int level);
}
