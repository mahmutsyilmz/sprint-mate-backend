package com.sprintmate.repository;

import com.sprintmate.model.ProjectTheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for ProjectTheme entity operations.
 */
@Repository
public interface ProjectThemeRepository extends JpaRepository<ProjectTheme, UUID> {

    List<ProjectTheme> findByActiveTrue();

    Optional<ProjectTheme> findByCode(String code);

    List<ProjectTheme> findByCodeIn(Set<String> codes);
}
