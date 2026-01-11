package com.sprintmate.service;

import com.sprintmate.dto.MatchCompletionRequest;
import com.sprintmate.dto.MatchCompletionResponse;
import com.sprintmate.dto.MatchResponse;
import com.sprintmate.dto.MatchStatusResponse;
import com.sprintmate.exception.ActiveMatchExistsException;
import com.sprintmate.exception.ResourceNotFoundException;
import com.sprintmate.exception.RoleNotSelectedException;
import com.sprintmate.model.*;
import com.sprintmate.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing developer matching operations.
 * 
 * Business Intent:
 * Implements the core matching algorithm that pairs frontend and backend developers.
 * Uses FIFO queue - first user to start waiting gets matched first.
 * Ensures atomic transactions when creating matches to maintain data consistency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchProjectRepository matchProjectRepository;
    private final MatchCompletionRepository matchCompletionRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectGeneratorService projectGeneratorService;

    private static final String MOCK_MEETING_URL = "https://meet.google.com/mock-id";
    private static final int PROJECT_DURATION_DAYS = 7;

    /**
     * Finds or queues a match for the current user.
     * Overload without topic - delegates to main method with null topic.
     */
    @Transactional
    public MatchStatusResponse findOrQueueMatch(UUID currentUserId) {
        return findOrQueueMatch(currentUserId, null);
    }

    /**
     * Finds or queues a match for the current user with optional topic preference.
     * 
     * Business Logic (FIFO Queue):
     * 1. Check if user already has an active match → throw exception
     * 2. Verify user has selected a role → throw exception if not
     * 3. Determine target role (opposite of user's role)
     * 4. Look for oldest waiting partner with target role
     * 5. If partner found:
     *    - Create match atomically
     *    - Generate AI project based on skills and topic
     *    - Clear both users' waitingSince
     *    - Return MATCHED status
     * 6. If no partner found:
     *    - Add current user to queue (set waitingSince)
     *    - Return WAITING status with queue position
     *
     * @param currentUserId The UUID of the user initiating the match
     * @param topic         Optional topic for AI project generation (e.g., "Fintech", "Sports")
     * @return MatchStatusResponse with either MATCHED details or WAITING status
     * @throws ResourceNotFoundException if user not found
     * @throws RoleNotSelectedException if user hasn't selected a role
     * @throws ActiveMatchExistsException if user already has an active match
     */
    @Transactional
    public MatchStatusResponse findOrQueueMatch(UUID currentUserId, String topic) {
        // Step 1: Find the current user
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        // Step 2: Check if user has selected a role
        if (currentUser.getRole() == null) {
            throw RoleNotSelectedException.forUser(currentUserId);
        }

        // Step 3: Check if user already has an active match
        if (matchRepository.existsActiveMatchForUser(currentUserId, MatchStatus.ACTIVE)) {
            throw ActiveMatchExistsException.forUser(currentUserId);
        }

        // Step 4: Determine the target role (opposite of current user's role)
        RoleName targetRole = getOppositeRole(currentUser.getRole());

        // Step 5: Look for oldest waiting partner with target role
        var partnerOpt = userRepository.findOldestWaitingByRole(targetRole.name(), currentUserId);

        if (partnerOpt.isPresent()) {
            // Partner found! Create match
            User partner = partnerOpt.get();
            log.info("Found waiting partner {} for user {}", partner.getId(), currentUserId);

            // Create the match (atomic transaction)
            Match match = createMatch();
            createParticipants(match, currentUser, partner);
            MatchProject matchProject = assignProject(match, currentUser, partner, topic);

            // Clear waitingSince for both users (they're now matched)
            currentUser.setWaitingSince(null);
            partner.setWaitingSince(null);
            userRepository.save(currentUser);
            userRepository.save(partner);

            log.info("Created match {} between {} and {} with project {}", 
                     match.getId(), currentUserId, partner.getId(), matchProject.getProjectTemplate().getTitle());

            return MatchStatusResponse.matched(
                match.getId(),
                match.getCommunicationLink(),
                buildPartnerName(partner),
                partner.getRole().name(),
                matchProject.getProjectTemplate().getTitle(),
                matchProject.getProjectTemplate().getDescription()
            );
        } else {
            // No partner available - add to queue
            LocalDateTime now = LocalDateTime.now();
            
            // Check if already waiting
            if (currentUser.getWaitingSince() == null) {
                currentUser.setWaitingSince(now);
                userRepository.save(currentUser);
                log.info("User {} joined the waiting queue at {}", currentUserId, now);
            } else {
                log.info("User {} is already in queue since {}", currentUserId, currentUser.getWaitingSince());
            }

            // Calculate queue position
            int queuePosition = getQueuePosition(currentUser);

            return MatchStatusResponse.waiting(currentUser.getWaitingSince(), queuePosition);
        }
    }

    /**
     * Calculates user's position in the waiting queue.
     */
    private int getQueuePosition(User user) {
        // Count how many users with the same role joined the queue before this user
        // For simplicity, we return 1 if they're in queue (can be enhanced later)
        return 1;
    }

    /**
     * Cancels waiting in the queue.
     *
     * @param userId The user who wants to leave the queue
     */
    @Transactional
    public void cancelWaiting(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        if (user.getWaitingSince() != null) {
            user.setWaitingSince(null);
            userRepository.save(user);
            log.info("User {} left the waiting queue", userId);
        }
    }

    /**
     * Legacy method - wraps new queue-based method for backward compatibility.
     * @deprecated Use findOrQueueMatch instead
     */
    @Transactional
    public MatchResponse findMatch(UUID currentUserId) {
        MatchStatusResponse status = findOrQueueMatch(currentUserId);
        
        if ("WAITING".equals(status.status())) {
            // Return a response indicating waiting status
            return new MatchResponse(
                null,
                "WAITING",
                null,
                null,
                null,
                null,
                null
            );
        }
        
        return new MatchResponse(
            status.matchId(),
            status.status(),
            status.meetingUrl(),
            status.partnerName(),
            status.partnerRole(),
            status.projectTitle(),
            status.projectDescription()
        );
    }

    /**
     * Completes an active match and saves the project repository URL.
     * 
     * Business Logic:
     * 1. Verify match exists
     * 2. Verify match status is ACTIVE
     * 3. Security: Verify currentUserId is a participant in this match
     * 4. Update match status to COMPLETED
     * 5. Create and save MatchCompletion record
     * 
     * After completion, both users are free to search for new matches.
     *
     * @param matchId       The UUID of the match to complete
     * @param request       The completion request with optional repo URL
     * @param currentUserId The UUID of the user completing the match
     * @return MatchCompletionResponse with completion details
     * @throws ResourceNotFoundException if match not found
     * @throws IllegalStateException if match is not in ACTIVE status
     * @throws AccessDeniedException if currentUserId is not a participant
     */
    @Transactional
    public MatchCompletionResponse completeMatch(UUID matchId, MatchCompletionRequest request, UUID currentUserId) {
        // Step 1: Find the match
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));

        // Step 2: Verify match status is ACTIVE
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new IllegalStateException(
                String.format("Match %s cannot be completed. Current status: %s. Only ACTIVE matches can be completed.",
                    matchId, match.getStatus())
            );
        }

        // Step 3: Security check - verify user is a participant
        List<MatchParticipant> participants = matchParticipantRepository.findByMatch(match);
        boolean isParticipant = participants.stream()
            .anyMatch(p -> p.getUser().getId().equals(currentUserId));

        if (!isParticipant) {
            log.warn("User {} attempted to complete match {} but is not a participant", currentUserId, matchId);
            throw new AccessDeniedException(
                String.format("User %s is not authorized to complete match %s", currentUserId, matchId)
            );
        }

        // Step 4: Update match status to COMPLETED
        match.setStatus(MatchStatus.COMPLETED);
        matchRepository.save(match);

        // Step 5: Create and save MatchCompletion record
        String repoUrl = request != null ? request.githubRepoUrl() : null;
        LocalDateTime completedAt = LocalDateTime.now();
        MatchCompletion completion = MatchCompletion.builder()
            .match(match)
            .repoUrl(repoUrl)
            .build();
        matchCompletionRepository.save(completion);

        log.info("Match {} completed by user {} with repo URL: {}", 
                 matchId, currentUserId, repoUrl != null ? repoUrl : "none");

        return MatchCompletionResponse.of(matchId, completedAt, repoUrl);
    }

    /**
     * Gets the opposite role for matching.
     * Frontend developers are matched with Backend developers and vice versa.
     */
    private RoleName getOppositeRole(RoleName role) {
        return role == RoleName.FRONTEND ? RoleName.BACKEND : RoleName.FRONTEND;
    }

    /**
     * Creates a new Match record with ACTIVE status.
     */
    private Match createMatch() {
        Match match = Match.builder()
            .status(MatchStatus.ACTIVE)
            .communicationLink(MOCK_MEETING_URL)
            .build();
        return matchRepository.save(match);
    }

    /**
     * Creates MatchParticipant records for both users.
     */
    private void createParticipants(Match match, User currentUser, User partner) {
        MatchParticipant currentParticipant = MatchParticipant.builder()
            .match(match)
            .user(currentUser)
            .participantRole(toParticipantRole(currentUser.getRole()))
            .build();

        MatchParticipant partnerParticipant = MatchParticipant.builder()
            .match(match)
            .user(partner)
            .participantRole(toParticipantRole(partner.getRole()))
            .build();

        matchParticipantRepository.save(currentParticipant);
        matchParticipantRepository.save(partnerParticipant);
    }

    /**
     * Converts RoleName to ParticipantRole.
     */
    private ParticipantRole toParticipantRole(RoleName roleName) {
        return roleName == RoleName.FRONTEND ? ParticipantRole.FRONTEND : ParticipantRole.BACKEND;
    }

    /**
     * Assigns a project template to the match.
     * Uses AI generation if topic is provided, falls back to random template.
     * 
     * @param match       The match to assign project to
     * @param currentUser One of the matched users
     * @param partner     The other matched user
     * @param topic       Optional topic for AI generation
     */
    private MatchProject assignProject(Match match, User currentUser, User partner, String topic) {
        ProjectTemplate template;
        
        // Determine frontend and backend users based on roles
        User frontendUser = currentUser.getRole() == RoleName.FRONTEND ? currentUser : partner;
        User backendUser = currentUser.getRole() == RoleName.BACKEND ? currentUser : partner;
        
        // Try AI generation first
        template = projectGeneratorService.generateProject(frontendUser, backendUser, topic);
        
        // Fallback to random template if AI generation returns null
        if (template == null) {
            log.info("AI generation returned null, falling back to random template");
            template = projectService.getRandomTemplate();
        }

        MatchProject matchProject = MatchProject.builder()
            .match(match)
            .projectTemplate(template)
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(PROJECT_DURATION_DAYS))
            .build();

        return matchProjectRepository.save(matchProject);
    }

    /**
     * Builds the partner's display name.
     */
    private String buildPartnerName(User partner) {
        if (partner.getSurname() != null && !partner.getSurname().isEmpty()) {
            return partner.getName() + " " + partner.getSurname();
        }
        return partner.getName();
    }
}
