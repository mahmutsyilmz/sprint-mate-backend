// package com.sprintmate.config;

// import com.sprintmate.model.*;
// import com.sprintmate.repository.ProjectTemplateRepository;
// import com.sprintmate.repository.UserRepository;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.stereotype.Component;
// import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDateTime;
// import java.util.Set;
// import java.util.UUID;

// /**
//  * Data initializer that seeds the database with project templates and dummy users
//  * on application startup. Used for MVP testing of matching logic.
//  *
//  * This initializer runs after Spring context is ready and populates:
//  * - 3 Project Templates (E-Commerce MVP, Weather Dashboard, Task Tracker)
//  * - 10 Dummy Users (5 Frontend, 5 Backend) with roles set directly on User entity
//  */
// @Component
// public class DataInitializer implements CommandLineRunner {

//     private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

//     private final ProjectTemplateRepository projectTemplateRepository;
//     private final UserRepository userRepository;

//     public DataInitializer(
//             ProjectTemplateRepository projectTemplateRepository,
//             UserRepository userRepository) {
//         this.projectTemplateRepository = projectTemplateRepository;
//         this.userRepository = userRepository;
//     }

//     @Override
//     @Transactional
//     public void run(String... args) {
//         logger.info("Starting data initialization...");

//         seedProjectTemplates();
//         seedDummyUsers();

//         logger.info("Data initialization completed successfully!");
//     }

//     /**
//      * Seeds 3 project templates for testing matching logic.
//      */
//     private void seedProjectTemplates() {
//         if (projectTemplateRepository.count() == 0) {
//             logger.info("Seeding project templates...");

//             ProjectTemplate ecommerce = ProjectTemplate.builder()
//                     .title("E-Commerce MVP")
//                     .description("Build a full-stack e-commerce application with product catalog, " +
//                             "shopping cart, and checkout flow. Frontend: React with TypeScript, " +
//                             "TailwindCSS. Backend: Java Spring Boot, PostgreSQL. " +
//                             "Features: User authentication, product search, order management.")
//                     .build();
//             projectTemplateRepository.save(ecommerce);

//             ProjectTemplate weather = ProjectTemplate.builder()
//                     .title("Weather Dashboard")
//                     .description("Create a weather dashboard that displays current conditions " +
//                             "and forecasts for multiple locations. Frontend: Vue.js with Vuetify. " +
//                             "Backend: Node.js with Express, Redis caching. " +
//                             "Features: Location search, favorites, weather alerts, data visualization.")
//                     .build();
//             projectTemplateRepository.save(weather);

//             ProjectTemplate taskTracker = ProjectTemplate.builder()
//                     .title("Task Tracker")
//                     .description("Develop a collaborative task tracking application with " +
//                             "kanban boards and team features. Frontend: Angular with Material UI. " +
//                             "Backend: Go with Gin framework, MongoDB. " +
//                             "Features: Drag-and-drop, real-time updates, team collaboration, due dates.")
//                     .build();
//             projectTemplateRepository.save(taskTracker);

//             logger.info("Project templates seeded: E-Commerce MVP, Weather Dashboard, Task Tracker");
//         } else {
//             logger.info("Project templates already exist, skipping seed.");
//         }
//     }

//     /**
//      * Seeds 10 dummy users (5 Frontend, 5 Backend) with role set directly on User entity.
//      * All dummy users are added to the waiting queue with staggered times for FIFO testing.
//      */
//     private void seedDummyUsers() {
//         if (userRepository.count() == 0) {
//             logger.info("Seeding dummy users...");

//             // Base time for queue ordering - older times = first in queue (FIFO)
//             LocalDateTime baseTime = LocalDateTime.now().minusHours(1);

//             // Frontend skills for realistic AI project generation testing
//             Set<String> frontendSkills = Set.of("React", "TypeScript", "Tailwind", "Vite");

//             // Backend skills for realistic AI project generation testing
//             Set<String> backendSkills = Set.of("Java", "Spring Boot", "PostgreSQL", "Docker");

//             // Create 5 Frontend developers (all waiting in queue with staggered times)
//             for (int i = 1; i <= 5; i++) {
//                 User frontendUser = User.builder()
//                         .id(UUID.randomUUID())
//                         .name("Bot Frontend " + i)
//                         .surname("Developer")
//                         .githubUrl("https://github.com/fake_fe_" + i)
//                         .role(RoleName.FRONTEND)
//                         .skills(new java.util.HashSet<>(frontendSkills))
//                         // Stagger queue times: FE1 joined 60min ago, FE2 55min ago, etc.
//                         .waitingSince(baseTime.plusMinutes((i - 1) * 5L))
//                         .build();
//                 userRepository.save(frontendUser);

//                 logger.debug("Created frontend user: {} with skills {} (waiting since {})",
//                         frontendUser.getName(), frontendUser.getSkills(), frontendUser.getWaitingSince());
//             }

//             // Create 5 Backend developers (all waiting in queue with staggered times)
//             for (int i = 1; i <= 5; i++) {
//                 User backendUser = User.builder()
//                         .id(UUID.randomUUID())
//                         .name("Bot Backend " + i)
//                         .surname("Developer")
//                         .githubUrl("https://github.com/fake_be_" + i)
//                         .role(RoleName.BACKEND)
//                         .skills(new java.util.HashSet<>(backendSkills))
//                         // Stagger queue times: BE1 joined 60min ago, BE2 55min ago, etc.
//                         .waitingSince(baseTime.plusMinutes((i - 1) * 5L))
//                         .build();
//                 userRepository.save(backendUser);

//                 logger.debug("Created backend user: {} with skills {} (waiting since {})",
//                         backendUser.getName(), backendUser.getSkills(), backendUser.getWaitingSince());
//             }

//             // Create real user: Mahmut Sami Yılmaz (NOT in queue - will join when they click find match)
//             User realUser = User.builder()
//                     .id(UUID.fromString("63357787-77fd-4175-9a41-e899dc4c100e"))
//                     .name("Mahmut Sami Yılmaz")
//                     .surname(null)
//                     .githubUrl("https://github.com/mahmutsyilmz")
//                     .role(null)          // No role yet
//                     .waitingSince(null)  // Not in queue
//                     .build();
//             userRepository.save(realUser);

//             logger.info("Dummy users seeded: 5 Frontend, 5 Backend developers (all in waiting queue)");
//             logger.info("Real user seeded: Mahmut Sami Yılmaz (not in queue)");
//         } else {
//             logger.info("Users already exist, skipping seed.");
//         }
//     }
// }
