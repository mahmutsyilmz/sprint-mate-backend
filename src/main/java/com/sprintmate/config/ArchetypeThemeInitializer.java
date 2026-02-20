package com.sprintmate.config;

import com.sprintmate.model.ProjectArchetype;
import com.sprintmate.model.ProjectTheme;
import com.sprintmate.repository.ProjectArchetypeRepository;
import com.sprintmate.repository.ProjectThemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Seeds the database with project archetypes and themes on startup.
 * Archetypes define structural patterns; themes define domain contexts.
 * Together they enable infinite unique project combinations via AI generation.
 * Also fixes column lengths that ddl-auto=update cannot widen.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ArchetypeThemeInitializer implements CommandLineRunner {

    private final ProjectArchetypeRepository archetypeRepository;
    private final ProjectThemeRepository themeRepository;
    private final DataSource dataSource;

    @Override
    @Transactional
    public void run(String... args) {
        fixColumnLengths();
        seedArchetypes();
        seedThemes();
    }

    /**
     * Widens columns that ddl-auto=update cannot alter.
     * Uses raw JDBC (outside JPA transaction) so the DDL commits immediately.
     */
    private void fixColumnLengths() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "IF COL_LENGTH('project_templates', 'description') IS NOT NULL " +
                "AND COL_LENGTH('project_templates', 'description') < 4000 " +
                "BEGIN ALTER TABLE project_templates ALTER COLUMN description NVARCHAR(2000); END"
            );
            log.info("Column length fix applied for project_templates.description");
        } catch (Exception e) {
            log.warn("Could not fix column lengths (may be fine on fresh DB): {}", e.getMessage());
        }
    }

    private void seedArchetypes() {
        if (archetypeRepository.count() > 0) {
            log.info("Project archetypes already exist, skipping seed.");
            return;
        }

        log.info("Seeding project archetypes...");

        seedArchetype("CRUD_APP", "CRUD Application",
                "Standard create-read-update-delete application with search, filtering, and data management capabilities.",
                "CRUD,REST,Pagination,Search,Forms,Validation",
                "REST",
                1, 3);

        seedArchetype("REAL_TIME_APP", "Real-Time Application",
                "Application with live updates via WebSocket or Server-Sent Events. Users see changes instantly without refreshing.",
                "WebSocket,EventDriven,LiveUpdate,Notifications",
                "REST,WebSocket",
                2, 4);

        seedArchetype("DATA_VISUALIZATION", "Data Visualization",
                "Dashboard-style application with charts, graphs, and data insights. Focuses on presenting data in meaningful ways.",
                "Charts,Analytics,Dashboard,Filters,DataAggregation",
                "REST",
                2, 4);

        seedArchetype("MARKETPLACE", "Marketplace",
                "Platform where users can list, browse, and interact around items or services. Includes search, profiles, and user-to-user interaction.",
                "Listing,Search,UserProfiles,Messaging,Categories",
                "REST",
                3, 5);

        seedArchetype("SOCIAL_FEED", "Social Feed",
                "Content-sharing platform with posting, reactions, following, and a personalized feed. Social interaction is the core.",
                "Feed,Reactions,UserRelations,ContentPosting,Infinite Scroll",
                "REST,WebSocket",
                2, 4);

        seedArchetype("GAMIFIED_APP", "Gamified Application",
                "Application that uses game mechanics like points, streaks, leaderboards, and achievements to engage users.",
                "Scoring,Leaderboard,Achievements,Streaks,Progress",
                "REST",
                2, 4);

        seedArchetype("DASHBOARD", "Dashboard",
                "Overview application with multiple panels, metrics, settings, and customizable views. Management and monitoring focused.",
                "Panels,Metrics,Filters,Settings,StatusCards",
                "REST",
                2, 3);

        seedArchetype("AUTOMATION_TOOL", "Automation Tool",
                "Tool that automates workflows with scheduling, triggers, and rule-based actions. Productivity and efficiency focused.",
                "Scheduling,Triggers,Workflows,Rules,Notifications",
                "REST",
                3, 5);

        log.info("Seeded {} project archetypes.", archetypeRepository.count());
    }

    private void seedThemes() {
        if (themeRepository.count() > 0) {
            log.info("Project themes already exist, skipping seed.");
            return;
        }

        log.info("Seeding project themes...");

        seedTheme("finance", "Finance",
                "Financial data, budgets, transactions, investments, spending analysis, and money management.",
                "budget,transaction,portfolio,account,payment");

        seedTheme("health", "Health & Fitness",
                "Workouts, nutrition, habits, wellness tracking, fitness goals, and health metrics.",
                "workout,exercise,meal,healthMetric,goal");

        seedTheme("education", "Education",
                "Courses, quizzes, progress tracking, learning paths, study materials, and knowledge sharing.",
                "course,quiz,lesson,progress,student");

        seedTheme("gaming", "Gaming",
                "Scores, matches, players, game sessions, challenges, and competitive features.",
                "player,match,score,leaderboard,challenge");

        seedTheme("social", "Social",
                "Posts, profiles, connections, messaging, sharing, and community building.",
                "post,comment,profile,connection,message");

        seedTheme("e-commerce", "E-Commerce",
                "Products, carts, orders, reviews, wishlists, and online shopping experiences.",
                "product,cart,order,review,category");

        seedTheme("productivity", "Productivity",
                "Tasks, projects, time tracking, goals, notes, and personal organization.",
                "task,project,timer,goal,note");

        seedTheme("entertainment", "Entertainment",
                "Movies, books, music, reviews, recommendations, and content discovery.",
                "movie,book,playlist,review,recommendation");

        log.info("Seeded {} project themes.", themeRepository.count());
    }

    private void seedArchetype(String code, String displayName, String structureDescription,
                               String componentPatterns, String apiPatterns,
                               int minComplexity, int maxComplexity) {
        ProjectArchetype archetype = ProjectArchetype.builder()
                .code(code)
                .displayName(displayName)
                .structureDescription(structureDescription)
                .componentPatterns(componentPatterns)
                .apiPatterns(apiPatterns)
                .minComplexity(minComplexity)
                .maxComplexity(maxComplexity)
                .active(true)
                .build();
        archetypeRepository.save(archetype);
        log.debug("Seeded archetype: {}", code);
    }

    private void seedTheme(String code, String displayName, String domainContext, String exampleEntities) {
        ProjectTheme theme = ProjectTheme.builder()
                .code(code)
                .displayName(displayName)
                .domainContext(domainContext)
                .exampleEntities(exampleEntities)
                .active(true)
                .build();
        themeRepository.save(theme);
        log.debug("Seeded theme: {}", code);
    }
}
