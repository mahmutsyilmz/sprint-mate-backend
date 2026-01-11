package com.sprintmate.mapper;

import com.sprintmate.dto.UserResponse;
import com.sprintmate.model.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between User entity and DTOs.
 * 
 * Business Intent:
 * Centralizes entity-to-DTO conversion logic to ensure consistent
 * data transformation across all services that handle User data.
 * Prevents entity details from leaking to API responses.
 */
@Component
public class UserMapper {

    /**
     * Converts User entity to UserResponse DTO.
     * Handles null role gracefully by returning null string.
     *
     * @param user The user entity to convert
     * @return UserResponse DTO with user data
     */
    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getGithubUrl(),
            user.getName(),
            user.getSurname(),
            user.getRole() != null ? user.getRole().name() : null,
            user.getBio()
        );
    }
}
