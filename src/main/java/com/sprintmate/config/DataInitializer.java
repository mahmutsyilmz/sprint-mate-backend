package com.sprintmate.config;

import com.sprintmate.model.*;
import com.sprintmate.repository.ProjectThemeRepository;
import com.sprintmate.repository.UserPreferenceRepository;
import com.sprintmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Seeds bot users into the waiting queue for local testing.
 * Enabled via: sprintmate.seed-bot-users=true in application.properties.
 *
 * Creates 2 Frontend and 2 Backend bot users with realistic skills and preferences,
 * all waiting in the queue. This allows a real user to immediately match when clicking
 * "Find Match" without needing a second real user.
 *
 * Runs AFTER ArchetypeThemeInitializer (@Order(2)) so themes are available for preferences.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "sprintmate.seed-bot-users", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ProjectThemeRepository themeRepository;
    private final DataSource dataSource;

    private static final List<String> BOT_LOGINS = List.of(
            "bot-fe-1", "bot-fe-2", "bot-be-1", "bot-be-2"
    );

    @Override
    @Transactional
    public void run(String... args) {
        boolean botsExist = userRepository.findByGithubUrl("https://github.com/bot-fe-1").isPresent();

        if (botsExist) {
            requeueBots();
            return;
        }

        seedBots();
    }

    /**
     * Clears old bot matches and puts bots back in the waiting queue.
     * This runs on every restart so bots are always available for testing.
     */
    private void requeueBots() {
        log.info("Re-queuing bot users for testing...");

        // Clear old matches involving bots via raw SQL
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Remove bot participations
            stmt.executeUpdate(
                "DELETE FROM match_participants WHERE user_id IN (" +
                "  SELECT id FROM users WHERE github_url LIKE 'https://github.com/bot-%')"
            );
            // Remove orphaned match_projects
            stmt.executeUpdate(
                "DELETE FROM match_projects WHERE match_id NOT IN (" +
                "  SELECT DISTINCT match_id FROM match_participants)"
            );
            // Remove orphaned matches
            stmt.executeUpdate(
                "DELETE FROM matches WHERE id NOT IN (" +
                "  SELECT DISTINCT match_id FROM match_participants)"
            );
            log.debug("Cleared old bot matches.");
        } catch (Exception e) {
            log.warn("Could not clear bot matches: {}", e.getMessage());
        }

        // Reset waitingSince for all bots
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(30);
        int index = 0;
        for (String login : BOT_LOGINS) {
            Optional<User> botOpt = userRepository.findByGithubUrl("https://github.com/" + login);
            if (botOpt.isPresent()) {
                User bot = botOpt.get();
                bot.setWaitingSince(baseTime.plusMinutes(index * 5L));
                userRepository.save(bot);
                index++;
            }
        }
        log.info("Re-queued {} bot users into waiting queue.", index);
    }

    private void seedBots() {
        log.info("Seeding bot users for local testing...");

        List<ProjectTheme> allThemes = themeRepository.findByActiveTrue();
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(30);

        // Frontend Bot 1 - React specialist
        User feBot1 = createBot("bot-fe-1", "Alex Frontend", RoleName.FRONTEND,
                Set.of("React", "TypeScript", "Tailwind CSS", "Vite"),
                baseTime);
        createPreference(feBot1, 2, "WebSocket, Redux", allThemes, Set.of("gaming", "social"));

        // Frontend Bot 2 - Vue specialist
        User feBot2 = createBot("bot-fe-2", "Jordan UI", RoleName.FRONTEND,
                Set.of("Vue.js", "JavaScript", "SCSS", "Pinia"),
                baseTime.plusMinutes(5));
        createPreference(feBot2, 1, "GraphQL", allThemes, Set.of("education", "productivity"));

        // Backend Bot 1 - Spring specialist
        User beBot1 = createBot("bot-be-1", "Sam Backend", RoleName.BACKEND,
                Set.of("Java", "Spring Boot", "PostgreSQL", "Docker"),
                baseTime.plusMinutes(10));
        createPreference(beBot1, 2, "Kafka, Redis", allThemes, Set.of("finance", "e-commerce"));

        // Backend Bot 2 - Node specialist
        User beBot2 = createBot("bot-be-2", "Riley Server", RoleName.BACKEND,
                Set.of("Node.js", "Express", "MongoDB", "Redis"),
                baseTime.plusMinutes(15));
        createPreference(beBot2, 3, "Microservices", allThemes, Set.of("health", "entertainment"));

        log.info("Seeded 4 bot users (2 FE + 2 BE) in waiting queue for testing.");
    }


    private User createBot(String githubLogin, String name, RoleName role,
                           Set<String> skills, LocalDateTime waitingSince) {
        User user = User.builder()
                .name(name)
                .githubUrl("https://github.com/" + githubLogin)
                .role(role)
                .skills(new HashSet<>(skills))
                .waitingSince(waitingSince)
                .build();
        return userRepository.save(user);
    }

    private void createPreference(User user, int difficulty, String learningGoals,
                                  List<ProjectTheme> allThemes, Set<String> themeCodes) {
        Set<ProjectTheme> preferred = new HashSet<>();
        for (ProjectTheme theme : allThemes) {
            if (themeCodes.contains(theme.getCode())) {
                preferred.add(theme);
            }
        }

        UserPreference pref = UserPreference.builder()
                .user(user)
                .difficultyPreference(difficulty)
                .learningGoals(learningGoals)
                .preferredThemes(preferred)
                .build();
        userPreferenceRepository.save(pref);
    }
}
