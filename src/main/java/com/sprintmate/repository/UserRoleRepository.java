package com.sprintmate.repository;

import com.sprintmate.model.RoleName;
import com.sprintmate.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for UserRole entity persistence operations.
 * Provides operations for managing user-role associations.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    /**
     * Finds all user-role associations for a specific role.
     * Useful for querying users by their assigned role.
     *
     * @param roleName The role name to filter by
     * @return List of UserRole associations for the given role
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.role.roleName = :roleName")
    List<UserRole> findByRoleName(@Param("roleName") RoleName roleName);

    /**
     * Counts the number of users with a specific role.
     *
     * @param roleName The role name to count
     * @return The number of users with the specified role
     */
    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.role.roleName = :roleName")
    long countByRoleName(@Param("roleName") RoleName roleName);
}
