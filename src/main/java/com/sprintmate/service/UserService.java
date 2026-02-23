package com.sprintmate.service;

import com.sprintmate.dto.UserPreferenceRequest;
import com.sprintmate.dto.UserResponse;
import com.sprintmate.dto.UserStatusResponse;
import com.sprintmate.dto.UserUpdateRequest;
import com.sprintmate.exception.InvalidOtpException;
import com.sprintmate.exception.InvalidRoleException;
import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.mapper.UserMapper;
import com.sprintmate.model.*;
import com.sprintmate.repository.MatchParticipantRepository;
import com.sprintmate.repository.MatchProjectRepository;
import com.sprintmate.repository.MatchRepository;
import com.sprintmate.repository.ProjectThemeRepository;
import com.sprintmate.repository.UserPreferenceRepository;
import com.sprintmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Service layer for User-related business operations.
 *
 * Business Intent:
 * Handles user management operations including role assignment, profile updates,
 * and the email verification flow (OTP generation and verification).
 * Ensures data consistency through transactional boundaries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchProjectRepository matchProjectRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ProjectThemeRepository projectThemeRepository;
    private final EmailNotificationService emailNotificationService;

    /**
     * Updates the role of a user.
     *
     * Business Intent:
     * Allows users to select their developer role (FRONTEND or BACKEND).
     * This is a critical step as it determines matching compatibility.
     *
     * @param userId   The UUID of the user to update
     * @param roleName The role name to assign (must be "FRONTEND" or "BACKEND")
     * @return UserResponse DTO with updated user data
     * @throws ResourceNotFoundException if user does not exist
     * @throws InvalidRoleException if role name is not valid
     */
    @Transactional
    public UserResponse updateUserRole(UUID userId, String roleName) {
        RoleName role = parseRoleName(roleName);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

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
     * Allows users to update their editable profile fields (name, bio, role, skills, email).
     * If the email is changed, emailVerified is automatically reset to false, requiring
     * the user to re-verify the new address.
     *
     * @param userId  The UUID of the user to update
     * @param request The update request containing new values
     * @return UserResponse DTO with updated user data
     * @throws ResourceNotFoundException if user does not exist
     */
    @Transactional
    public UserResponse updateUserProfile(UUID userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }

        if (request.role() != null && !request.role().isBlank()) {
            RoleName role = parseRoleName(request.role());
            user.setRole(role);
        }

        if (request.skills() != null) {
            user.getSkills().clear();
            user.getSkills().addAll(request.skills());
        }

        if (request.preference() != null) {
            updatePreference(user, request.preference());
        }

        // If email is provided and different from current, reset verification status
        if (request.email() != null && !request.email().isBlank()) {
            if (!request.email().equals(user.getEmail())) {
                user.setEmail(request.email());
                user.setEmailVerified(false);
                user.setEmailVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                log.info("Email changed for user {} — emailVerified reset to false", userId);
            }
        }

        User savedUser = userRepository.save(user);

        log.info("Updated profile for user {}", userId);

        return userMapper.toResponse(savedUser);
    }

    /**
     * Generates and sends an OTP to the given email address for verification.
     *
     * Business Intent:
     * Part of the email verification flow. Generates a 6-digit OTP with a 10-minute
     * expiry, saves it to the user record, and sends it asynchronously via email.
     * Can be called both during onboarding and when updating an email in the profile.
     *
     * @param userId The UUID of the user requesting verification
     * @param email  The email address to send the OTP to
     * @throws ResourceNotFoundException if user does not exist
     */
    @Transactional
    public void sendEmailVerification(UUID userId, String email) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String otp = String.format("%06d", new Random().nextInt(1_000_000));

        user.setEmail(email);
        user.setEmailVerified(false);
        user.setEmailVerificationCode(otp);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

        userRepository.save(user);

        emailNotificationService.sendVerificationEmail(email, user.getName(), otp);

        log.info("Sent verification code to {} for user {}", email, userId);
    }

    /**
     * Verifies the OTP code submitted by the user.
     *
     * Business Intent:
     * Completes the email verification flow. If the code is correct and not expired,
     * the user's email is marked as verified and the OTP fields are cleared.
     *
     * @param userId  The UUID of the user verifying their email
     * @param otpCode The 6-digit code submitted by the user
     * @throws ResourceNotFoundException if user does not exist
     * @throws InvalidOtpException if the code is wrong or expired
     */
    @Transactional
    public void verifyEmail(UUID userId, String otpCode) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getEmailVerificationCode() == null || !user.getEmailVerificationCode().equals(otpCode)) {
            throw new InvalidOtpException("Invalid verification code");
        }

        if (user.getVerificationCodeExpiresAt() == null ||
                user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("Verification code has expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);

        userRepository.save(user);

        log.info("Email verified for user {}", userId);
    }

    /**
     * Gets the complete status of a user including active match information.
     *
     * Business Intent:
     * Critical for session persistence — when a user logs in or refreshes,
     * the frontend needs to know if they're in an active match to redirect
     * them to the correct view, and whether their email is verified.
     *
     * @param githubUrl The GitHub profile URL of the user
     * @return UserStatusResponse with user data, active match info, and email verification status
     * @throws ResourceNotFoundException if user does not exist
     */
    @Transactional(readOnly = true)
    public UserStatusResponse getUserStatus(String githubUrl) {
        User user = userRepository.findByGithubUrl(githubUrl)
            .orElseThrow(() -> new ResourceNotFoundException("User", "githubUrl", githubUrl));

        Optional<Match> activeMatchOpt = matchRepository.findMatchByUserIdAndStatus(
            user.getId(), MatchStatus.ACTIVE);

        if (activeMatchOpt.isEmpty()) {
            boolean waiting = user.getWaitingSince() != null;
            Integer queuePos = waiting ? getQueuePosition(user) : null;
            return new UserStatusResponse(
                user.getId(),
                user.getGithubUrl(),
                user.getName(),
                user.getSurname(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getBio(),
                user.getSkills(),
                false,
                null,
                waiting,
                user.getWaitingSince(),
                queuePos,
                user.getEmail(),
                user.isEmailVerified()
            );
        }

        Match activeMatch = activeMatchOpt.get();

        List<MatchParticipant> participants = matchParticipantRepository.findByMatch(activeMatch);
        User partner = participants.stream()
            .map(MatchParticipant::getUser)
            .filter(u -> !u.getId().equals(user.getId()))
            .findFirst()
            .orElse(null);

        Optional<MatchProject> matchProjectOpt = matchProjectRepository.findByMatch(activeMatch);
        String projectTitle = null;
        String projectDescription = null;
        if (matchProjectOpt.isPresent()) {
            ProjectTemplate template = matchProjectOpt.get().getProjectTemplate();
            projectTitle = template.getTitle();
            projectDescription = template.getDescription();
        }

        String partnerName = partner != null ? buildPartnerName(partner) : null;
        String partnerRole = partner != null && partner.getRole() != null ? partner.getRole().name() : null;
        var partnerSkills = partner != null ? partner.getSkills() : null;

        UserStatusResponse.ActiveMatchInfo activeMatchInfo = new UserStatusResponse.ActiveMatchInfo(
            activeMatch.getId(),
            activeMatch.getCommunicationLink(),
            partnerName,
            partnerRole,
            partnerSkills,
            projectTitle,
            projectDescription
        );

        log.info("User {} has active match {}", user.getId(), activeMatch.getId());

        return new UserStatusResponse(
            user.getId(),
            user.getGithubUrl(),
            user.getName(),
            user.getSurname(),
            user.getRole() != null ? user.getRole().name() : null,
            user.getBio(),
            user.getSkills(),
            true,
            activeMatchInfo,
            false,
            null,
            null,
            user.getEmail(),
            user.isEmailVerified()
        );
    }

    private RoleName parseRoleName(String roleName) {
        try {
            return RoleName.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException(roleName);
        }
    }

    private int getQueuePosition(User user) {
        if (user.getWaitingSince() == null || user.getRole() == null) {
            return 0;
        }
        return userRepository.countByRoleAndWaitingSinceBefore(
            user.getRole(),
            user.getWaitingSince()
        ) + 1;
    }

    private void updatePreference(User user, UserPreferenceRequest prefRequest) {
        UserPreference pref = userPreferenceRepository.findByUserId(user.getId())
                .orElse(UserPreference.builder().user(user).build());

        pref.setDifficultyPreference(prefRequest.difficultyPreference());
        pref.setLearningGoals(prefRequest.learningGoals());

        if (prefRequest.preferredThemeCodes() != null) {
            var themes = new java.util.HashSet<>(
                    projectThemeRepository.findByCodeIn(prefRequest.preferredThemeCodes())
            );
            pref.setPreferredThemes(themes);
        }

        userPreferenceRepository.save(pref);
        user.setPreference(pref);
    }

    private String buildPartnerName(User partner) {
        if (partner.getSurname() != null && !partner.getSurname().isEmpty()) {
            return partner.getName() + " " + partner.getSurname();
        }
        return partner.getName();
    }
}
