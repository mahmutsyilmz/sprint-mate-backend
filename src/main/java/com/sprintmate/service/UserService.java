package com.sprintmate.service;

import com.sprintmate.dto.UserResponse;
import com.sprintmate.dto.UserUpdateRequest;
import com.sprintmate.exception.InvalidRoleException;
import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.mapper.UserMapper;
import com.sprintmate.model.RoleName;
import com.sprintmate.model.User;
import com.sprintmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service layer for User-related business operations.
 * 
 * Business Intent:
 * Handles user management operations including role assignment.
 * Ensures data consistency through transactional boundaries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Updates the role of a user.
     * 
     * Business Intent:
     * Allows users to select their developer role (FRONTEND or BACKEND).
     * This is a critical step as it determines matching compatibility.
     *
     * Flow:
     * 1. Validate role name is a valid RoleName enum
     * 2. Find user by ID (throw if not found)
     * 3. Update role and persist
     * 4. Return updated user as DTO
     *
     * @param userId   The UUID of the user to update
     * @param roleName The role name to assign (must be "FRONTEND" or "BACKEND")
     * @return UserResponse DTO with updated user data
     * @throws ResourceNotFoundException if user does not exist
     * @throws InvalidRoleException if role name is not valid
     */
    @Transactional
    public UserResponse updateUserRole(UUID userId, String roleName) {
        // Validate role name is a valid enum value
        RoleName role = parseRoleName(roleName);

        // Find user or throw not found
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Update role
        user.setRole(role);
        User savedUser = userRepository.save(user);

        log.info("Updated role for user {} to {}", userId, role);

        return userMapper.toResponse(savedUser);
    }

    /**
     * Finds a user by their GitHub URL.
     * 
     * Business Intent:
     * Used during authentication to get the local user record
     * that corresponds to the authenticated GitHub user.
     *
     * @param githubUrl The GitHub profile URL
     * @return UserResponse DTO if found
     * @throws ResourceNotFoundException if user does not exist
     */
    @Transactional(readOnly = true)
    public UserResponse findByGithubUrl(String githubUrl) {
        User user = userRepository.findByGithubUrl(githubUrl)
            .orElseThrow(() -> new ResourceNotFoundException("User", "githubUrl", githubUrl));

        return userMapper.toResponse(user);
    }

    /**
     * Updates the profile of a user.
     * 
     * Business Intent:
     * Allows users to update their editable profile fields (name, bio, role).
     * Only updates allowed fields to prevent unauthorized changes.
     *
     * Flow:
     * 1. Find user by ID (throw if not found)
     * 2. Update allowed fields (name, bio, role if provided)
     * 3. Persist changes
     * 4. Return updated user as DTO
     *
     * @param userId  The UUID of the user to update
     * @param request The update request containing new values
     * @return UserResponse DTO with updated user data
     * @throws ResourceNotFoundException if user does not exist
     */
    @Transactional
    public UserResponse updateUserProfile(UUID userId, UserUpdateRequest request) {
        // Find user or throw not found
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Update allowed fields
        user.setName(request.name());
        user.setBio(request.bio());
        
        // Update role if provided
        if (request.role() != null && !request.role().isBlank()) {
            RoleName role = parseRoleName(request.role());
            user.setRole(role);
        }

        // Update skills if provided (replaces old set with new one)
        if (request.skills() != null) {
            user.getSkills().clear();
            user.getSkills().addAll(request.skills());
        }

        User savedUser = userRepository.save(user);

        log.info("Updated profile for user {}", userId);

        return userMapper.toResponse(savedUser);
    }

    /**
     * Parses a role name string to RoleName enum.
     * 
     * @param roleName The role name string
     * @return The corresponding RoleName enum
     * @throws InvalidRoleException if the role name is not valid
     */
    private RoleName parseRoleName(String roleName) {
        try {
            return RoleName.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException(roleName);
        }
    }
}
