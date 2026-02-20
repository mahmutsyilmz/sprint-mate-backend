package com.sprintmate.config;

import com.sprintmate.model.ProjectIdea;
import com.sprintmate.repository.ProjectIdeaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the database with fun, portfolio-worthy project ideas on startup.
 * @deprecated Replaced by {@link ArchetypeThemeInitializer} which uses archetype + theme system.
 * Enable with sprintmate.seed-legacy-ideas=true if needed for backward compatibility.
 */
@Deprecated
@Component
@ConditionalOnProperty(name = "sprintmate.seed-legacy-ideas", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class ProjectIdeaInitializer implements CommandLineRunner {

    private final ProjectIdeaRepository projectIdeaRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (projectIdeaRepository.count() > 0) {
            log.info("Project ideas already exist, skipping seed.");
            return;
        }

        log.info("Seeding fun project ideas...");

        // Social & Communication
        seedIdea("Social", "Mini Twitter Clone",
            "Build your own micro-blogging platform!",
            "A simplified social platform where users can post short messages, follow others, and like posts. Think Twitter but focused on the core features.",
            "User authentication, Post creation with character limit, Follow/unfollow system, Like functionality, Chronological feed",
            "Real-time notifications, Hashtag support",
            "A developer wants to share coding tips with their followers",
            "Shows you understand social features, real-time data, and user relationships",
            3, "social,real-time,crud");

        seedIdea("Social", "Anonymous Confession Board",
            "Create a safe space for anonymous sharing!",
            "A platform where users can post anonymous confessions, others can react with emojis, and popular posts rise to the top.",
            "Anonymous posting, Emoji reactions, Trending algorithm, Report/moderation, Categories/tags",
            "Time-limited posts, Verification system",
            "College students sharing study struggles anonymously",
            "Demonstrates content moderation, ranking algorithms, and privacy considerations",
            2, "anonymous,voting,moderation");

        // Productivity
        seedIdea("Productivity", "Habit Tracker with Streaks",
            "Help people build better habits!",
            "A habit tracking app that gamifies personal development with streaks, achievements, and progress visualization.",
            "Create/edit habits, Daily check-ins, Streak tracking, Calendar view, Statistics dashboard",
            "Achievements/badges, Reminder notifications",
            "Someone tracking their workout routine and water intake daily",
            "Shows gamification, data visualization, and mobile-first design",
            2, "gamification,tracking,charts");

        seedIdea("Productivity", "Pomodoro Focus App",
            "Boost productivity with focused work sessions!",
            "A Pomodoro timer app with task lists, session history, and productivity analytics.",
            "Customizable timer, Task list integration, Session history, Daily/weekly stats, Focus mode UI",
            "Spotify integration, Team focus rooms",
            "A developer focusing on coding tasks with 25-min sprints",
            "Demonstrates timer logic, state management, and clean UX",
            2, "timer,focus,analytics");

        // Gaming & Fun
        seedIdea("Gaming", "Multiplayer Quiz Battle",
            "Challenge friends with trivia!",
            "A real-time multiplayer quiz game where players compete head-to-head answering trivia questions.",
            "Question bank management, Real-time game rooms, Scoring system, Leaderboards, Category selection",
            "Custom quiz creation, Tournament mode",
            "Friends competing to see who knows more about movies",
            "Showcases real-time WebSockets, competitive features, and engaging UX",
            3, "real-time,multiplayer,gaming");

        seedIdea("Gaming", "Emoji Story Game",
            "Express stories using only emojis!",
            "A party game where one player creates a story using emojis and others guess what it means.",
            "Room creation, Emoji palette, Voting system, Round management, Score tracking",
            "Custom emoji packs, Team mode",
            "Friends at a party guessing movie plots from emoji sequences",
            "Fun project showing creativity, real-time sync, and social gaming",
            2, "party-game,creative,voting");

        // Finance
        seedIdea("Finance", "Expense Split Calculator",
            "Never argue about bills again!",
            "An app for groups to track shared expenses and calculate who owes whom with minimal transactions.",
            "Group creation, Expense logging, Smart debt simplification, Settlement tracking, Expense categories",
            "Receipt scanning, Payment reminders",
            "Roommates splitting rent, utilities, and groceries fairly",
            "Demonstrates complex calculations, group dynamics, and practical utility",
            3, "fintech,calculation,groups");

        seedIdea("Finance", "Subscription Tracker",
            "Take control of your recurring payments!",
            "Track all your subscriptions in one place with spending insights and renewal reminders.",
            "Add/edit subscriptions, Monthly/yearly views, Spending analytics, Renewal calendar, Category breakdown",
            "Bank integration, Cancellation suggestions",
            "Someone realizing they're paying for 12 streaming services",
            "Shows practical value, data visualization, and calendar features",
            2, "tracking,calendar,analytics");

        // Content & Media
        seedIdea("Content", "Recipe Sharing Platform",
            "Share and discover delicious recipes!",
            "A community platform for food lovers to share recipes, save favorites, and plan meals.",
            "Recipe CRUD with ingredients, Search with filters, Favorites/bookmarks, User profiles, Rating system",
            "Meal planning, Shopping list generator",
            "A home cook sharing their grandmother's secret pasta recipe",
            "Demonstrates rich content, search/filter, and community features",
            3, "community,search,content");

        seedIdea("Content", "Code Snippet Manager",
            "Never lose that perfect code snippet again!",
            "A personal library for developers to save, organize, and quickly access code snippets.",
            "Snippet CRUD with syntax highlighting, Tags and categories, Full-text search, Copy to clipboard, Language detection",
            "Public sharing, VS Code extension",
            "A developer organizing their most-used React hooks",
            "Perfect for showing you understand developer tools and UX",
            2, "developer-tools,search,organization");

        // E-Commerce
        seedIdea("E-Commerce", "Local Marketplace",
            "Buy and sell within your community!",
            "A simplified marketplace for local buying and selling, like Craigslist but modern and safe.",
            "Listing creation with images, Category browsing, Search and filters, Chat between users, User ratings",
            "Location-based suggestions, Saved searches",
            "Someone selling their old gaming console to a neighbor",
            "Shows marketplace dynamics, user trust systems, and messaging",
            3, "marketplace,chat,location");

        seedIdea("E-Commerce", "Wishlist Sharing App",
            "Share your wishlist for any occasion!",
            "Create and share wishlists for birthdays, holidays, or any occasion. Perfect for gift coordination.",
            "Wishlist creation, Item adding from URLs, Sharing links, Gift claiming (hidden from owner), Multiple lists",
            "Price tracking, Group gifting",
            "A family coordinating Christmas gifts without duplicates",
            "Demonstrates link parsing, privacy logic, and sharing features",
            2, "wishlist,sharing,events");

        // Health & Lifestyle
        seedIdea("Health", "Workout Log & Progress",
            "Track your fitness journey!",
            "A workout logging app with exercise library, progress tracking, and personal records.",
            "Exercise database, Workout logging, Progress charts, Personal records, Workout templates",
            "Social sharing, AI workout suggestions",
            "A gym-goer tracking their bench press progress over months",
            "Shows data tracking, visualization, and template systems",
            2, "fitness,tracking,charts");

        seedIdea("Lifestyle", "Book Reading Tracker",
            "Organize your reading life!",
            "Track books you're reading, want to read, and have finished with reviews and stats.",
            "Book search/add, Reading status, Reviews and ratings, Reading goals, Statistics",
            "Book club features, Goodreads import",
            "A bookworm tracking their goal to read 50 books this year",
            "Shows third-party API integration and goal tracking",
            2, "books,tracking,goals");

        // Utility
        seedIdea("Utility", "Link Shortener with Analytics",
            "Shorten links and track clicks!",
            "Create short URLs and see detailed analytics about who clicked them.",
            "URL shortening, Click tracking, Geographic data, Custom aliases, QR code generation",
            "Expiring links, Password protection",
            "A marketer tracking campaign link performance",
            "Demonstrates URL handling, analytics, and real-world utility",
            2, "analytics,urls,tracking");

        seedIdea("Utility", "Bookmark Manager with Tags",
            "Organize your internet finds!",
            "Save, organize, and search through your bookmarks with tags and collections.",
            "Bookmark CRUD, Tagging system, Full-text search, Import/export, Collections/folders",
            "Browser extension, Auto-categorization",
            "A researcher organizing hundreds of article bookmarks",
            "Shows organization systems, search, and browser integration concepts",
            2, "bookmarks,organization,search");

        log.info("Seeded {} fun project ideas!", projectIdeaRepository.count());
    }

    private void seedIdea(String category, String name, String pitch, String coreConcept,
                          String keyFeatures, String bonusFeatures, String exampleUseCase,
                          String portfolioValue, int difficulty, String tags) {
        ProjectIdea idea = ProjectIdea.builder()
                .category(category)
                .name(name)
                .pitch(pitch)
                .coreConcept(coreConcept)
                .keyFeatures(keyFeatures)
                .bonusFeatures(bonusFeatures)
                .exampleUseCase(exampleUseCase)
                .portfolioValue(portfolioValue)
                .difficulty(difficulty)
                .tags(tags)
                .active(true)
                .build();
        projectIdeaRepository.save(idea);
        log.debug("Seeded project idea: {}", name);
    }
}
