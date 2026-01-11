package com.sprintmate.repository;

import com.sprintmate.model.Role;
import com.sprintmate.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Role entity persistence operations.
 * Provides lookup operations for role reference data.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Finds a role by its name.
     *
     * @param roleName The role name (FRONTEND or BACKEND)
     * @return Optional containing the Role if found, empty otherwise
     */
    Optional<Role> findByRoleName(RoleName roleName);
}
