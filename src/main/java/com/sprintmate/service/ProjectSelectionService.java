package com.sprintmate.service;

import com.sprintmate.model.ProjectArchetype;
import com.sprintmate.model.ProjectTheme;
import com.sprintmate.model.User;
import com.sprintmate.model.UserPreference;
import com.sprintmate.repository.ProjectArchetypeRepository;
import com.sprintmate.repository.ProjectThemeRepository;
import com.sprintmate.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart selection algorithm for choosing archetype + theme based on both users' preferences.
 *
 * Business Intent:
 * When two users are matched, this service intersects their preferences to pick
 * the best archetype-theme combination for AI project generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectSelectionService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final ProjectArchetypeRepository archetypeRepository;
    private final ProjectThemeRepository themeRepository;

    private static final int DEFAULT_COMPLEXITY = 2;
    private final Random random = new Random();

    /**
     * Result of the selection algorithm.
     */
    public record SelectionResult(
        ProjectArchetype archetype,
        ProjectTheme theme,
        int targetComplexity,
        String frontendLearningGoals,
        String backendLearningGoals
    ) {}

    /**
     * Selects archetype + theme based on both users' preferences.
     *
     * Algorithm:
     * 1. Load both users' preferences (fallback to defaults if missing)
     * 2. Compute target complexity = avg of both users' difficulty preferences
     * 3. Filter archetypes by complexity range, pick randomly
     * 4. Intersect preferred themes; if empty, use union; if still empty, random from all
     */
    public SelectionResult select(User frontendUser, User backendUser) {
        log.info("Selecting archetype+theme for {} (FE) and {} (BE)",
                frontendUser.getName(), backendUser.getName());

        Optional<UserPreference> fePref = userPreferenceRepository.findByUserId(frontendUser.getId());
        Optional<UserPreference> bePref = userPreferenceRepository.findByUserId(backendUser.getId());

        int targetComplexity = calculateComplexity(fePref.orElse(null), bePref.orElse(null));
        ProjectArchetype archetype = selectArchetype(targetComplexity);
        ProjectTheme theme = selectTheme(fePref.orElse(null), bePref.orElse(null));

        String feLearningGoals = fePref.map(UserPreference::getLearningGoals).orElse(null);
        String beLearningGoals = bePref.map(UserPreference::getLearningGoals).orElse(null);

        log.info("Selected archetype: {} ({}), theme: {} ({}), complexity: {}",
                archetype.getCode(), archetype.getDisplayName(),
                theme.getCode(), theme.getDisplayName(),
                targetComplexity);

        return new SelectionResult(archetype, theme, targetComplexity, feLearningGoals, beLearningGoals);
    }

    /**
     * Calculates target complexity from both users' difficulty preferences.
     * Average if both exist, single value if one exists, default (2) if neither.
     */
    int calculateComplexity(UserPreference fePref, UserPreference bePref) {
        Integer feDiff = fePref != null ? fePref.getDifficultyPreference() : null;
        Integer beDiff = bePref != null ? bePref.getDifficultyPreference() : null;

        if (feDiff != null && beDiff != null) {
            return (int) Math.round((feDiff + beDiff) / 2.0);
        } else if (feDiff != null) {
            return feDiff;
        } else if (beDiff != null) {
            return beDiff;
        }
        return DEFAULT_COMPLEXITY;
    }

    /**
     * Selects an archetype matching the target complexity.
     * Falls back to widened range (+/-1) then all active archetypes.
     */
    ProjectArchetype selectArchetype(int targetComplexity) {
        List<ProjectArchetype> matching = archetypeRepository.findByComplexityLevel(targetComplexity);

        if (matching.isEmpty()) {
            // Widen range by +/-1
            int lower = Math.max(1, targetComplexity - 1);
            int upper = Math.min(5, targetComplexity + 1);
            matching = archetypeRepository.findByActiveTrue().stream()
                    .filter(a -> a.getMinComplexity() <= upper && a.getMaxComplexity() >= lower)
                    .collect(Collectors.toList());
        }

        if (matching.isEmpty()) {
            matching = archetypeRepository.findByActiveTrue();
        }

        return matching.get(random.nextInt(matching.size()));
    }

    /**
     * Selects a theme based on both users' preferred themes.
     * Priority: intersection > union > all active themes.
     */
    ProjectTheme selectTheme(UserPreference fePref, UserPreference bePref) {
        Set<String> feThemeCodes = extractThemeCodes(fePref);
        Set<String> beThemeCodes = extractThemeCodes(bePref);

        // Try intersection first
        if (!feThemeCodes.isEmpty() && !beThemeCodes.isEmpty()) {
            Set<String> intersection = new HashSet<>(feThemeCodes);
            intersection.retainAll(beThemeCodes);

            if (!intersection.isEmpty()) {
                log.debug("Using theme intersection: {}", intersection);
                return pickRandomThemeByCode(intersection);
            }
        }

        // Try union
        Set<String> union = new HashSet<>();
        union.addAll(feThemeCodes);
        union.addAll(beThemeCodes);

        if (!union.isEmpty()) {
            log.debug("Using theme union: {}", union);
            return pickRandomThemeByCode(union);
        }

        // Fallback to any active theme
        log.debug("No theme preferences, using random active theme");
        List<ProjectTheme> allThemes = themeRepository.findByActiveTrue();
        return allThemes.get(random.nextInt(allThemes.size()));
    }

    private Set<String> extractThemeCodes(UserPreference pref) {
        if (pref == null || pref.getPreferredThemes() == null || pref.getPreferredThemes().isEmpty()) {
            return Set.of();
        }
        return pref.getPreferredThemes().stream()
                .map(ProjectTheme::getCode)
                .collect(Collectors.toSet());
    }

    private ProjectTheme pickRandomThemeByCode(Set<String> codes) {
        List<ProjectTheme> themes = themeRepository.findByCodeIn(codes);
        if (themes.isEmpty()) {
            List<ProjectTheme> allThemes = themeRepository.findByActiveTrue();
            return allThemes.get(random.nextInt(allThemes.size()));
        }
        return themes.get(random.nextInt(themes.size()));
    }
}
