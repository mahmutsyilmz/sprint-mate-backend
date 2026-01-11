package com.sprintmate.config;

import com.sprintmate.model.*;
import com.sprintmate.repository.ProjectTemplateRepository;
import com.sprintmate.repository.RoleRepository;
import com.sprintmate.repository.UserRepository;
import com.sprintmate.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DataInitializer.
 * Verifies that after application startup, the database is properly
 * seeded with project templates and dummy users.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DataInitializer Integration Tests")
class DataInitializerTest {

    @Autowired
    private ProjectTemplateRepository projectTemplateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    @DisplayName("should_SeedRoles_When_ApplicationStarts")
    void should_SeedRoles_When_ApplicationStarts() {
        // Assert
        List<Role> roles = roleRepository.findAll();
        assertThat(roles).hasSize(2);
        
        Optional<Role> frontendRole = roleRepository.findByRoleName(RoleName.FRONTEND);
        Optional<Role> backendRole = roleRepository.findByRoleName(RoleName.BACKEND);
        
        assertThat(frontendRole).isPresent();
        assertThat(backendRole).isPresent();
    }

    @Test
    @DisplayName("should_Seed3ProjectTemplates_When_ApplicationStarts")
    void should_Seed3ProjectTemplates_When_ApplicationStarts() {
        // Assert
        List<ProjectTemplate> templates = projectTemplateRepository.findAll();
        assertThat(templates).hasSize(3);
        
        List<String> titles = templates.stream()
                .map(ProjectTemplate::getTitle)
                .collect(Collectors.toList());
        
        assertThat(titles).containsExactlyInAnyOrder(
                "E-Commerce MVP",
                "Weather Dashboard",
                "Task Tracker"
        );
    }

    @Test
    @DisplayName("should_SeedProjectTemplatesWithDescriptions_When_ApplicationStarts")
    void should_SeedProjectTemplatesWithDescriptions_When_ApplicationStarts() {
        // Assert
        List<ProjectTemplate> templates = projectTemplateRepository.findAll();
        
        for (ProjectTemplate template : templates) {
            assertThat(template.getDescription()).isNotNull();
            assertThat(template.getDescription()).isNotBlank();
            assertThat(template.getId()).isNotNull();
        }
    }

    @Test
    @DisplayName("should_Seed11Users_When_ApplicationStarts")
    void should_Seed11Users_When_ApplicationStarts() {
        // Assert - 10 dummy users + 1 real user (Mahmut Sami Yılmaz)
        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(11);
    }

    @Test
    @DisplayName("should_Seed5FrontendUsers_When_ApplicationStarts")
    void should_Seed5FrontendUsers_When_ApplicationStarts() {
        // Assert
        List<User> frontendUsers = userRepository.findAll().stream()
                .filter(user -> user.getRole() == RoleName.FRONTEND)
                .collect(Collectors.toList());
        
        assertThat(frontendUsers).hasSize(5);
        
        // Verify naming convention
        for (User user : frontendUsers) {
            assertThat(user.getName()).startsWith("Bot Frontend");
            assertThat(user.getGithubUrl()).startsWith("https://github.com/fake_fe_");
        }
    }

    @Test
    @DisplayName("should_Seed5BackendUsers_When_ApplicationStarts")
    void should_Seed5BackendUsers_When_ApplicationStarts() {
        // Assert
        List<User> backendUsers = userRepository.findAll().stream()
                .filter(user -> user.getRole() == RoleName.BACKEND)
                .collect(Collectors.toList());
        
        assertThat(backendUsers).hasSize(5);
        
        // Verify naming convention
        for (User user : backendUsers) {
            assertThat(user.getName()).startsWith("Bot Backend");
            assertThat(user.getGithubUrl()).startsWith("https://github.com/fake_be_");
        }
    }

    @Test
    @DisplayName("should_CreateUserRoleAssociations_When_ApplicationStarts")
    void should_CreateUserRoleAssociations_When_ApplicationStarts() {
        // Assert - Only dummy users have role associations (real user has no role yet)
        List<UserRole> userRoles = userRoleRepository.findAll();
        assertThat(userRoles).hasSize(10); // 5 frontend + 5 backend (real user has no role)

        // Verify frontend user-role associations
        long frontendCount = userRoleRepository.countByRoleName(RoleName.FRONTEND);
        assertThat(frontendCount).isEqualTo(5);

        // Verify backend user-role associations
        long backendCount = userRoleRepository.countByRoleName(RoleName.BACKEND);
        assertThat(backendCount).isEqualTo(5);
    }

    @Test
    @DisplayName("should_HaveQueryableUserRoles_When_ApplicationStarts")
    void should_HaveQueryableUserRoles_When_ApplicationStarts() {
        // Assert - Verify we can query users by role through UserRole
        List<UserRole> frontendUserRoles = userRoleRepository.findByRoleName(RoleName.FRONTEND);
        assertThat(frontendUserRoles).hasSize(5);
        
        for (UserRole userRole : frontendUserRoles) {
            assertThat(userRole.getUser()).isNotNull();
            assertThat(userRole.getRole()).isNotNull();
            assertThat(userRole.getRole().getRoleName()).isEqualTo(RoleName.FRONTEND);
            assertThat(userRole.getUser().getRole()).isEqualTo(RoleName.FRONTEND);
        }

        List<UserRole> backendUserRoles = userRoleRepository.findByRoleName(RoleName.BACKEND);
        assertThat(backendUserRoles).hasSize(5);
        
        for (UserRole userRole : backendUserRoles) {
            assertThat(userRole.getUser()).isNotNull();
            assertThat(userRole.getRole()).isNotNull();
            assertThat(userRole.getRole().getRoleName()).isEqualTo(RoleName.BACKEND);
            assertThat(userRole.getUser().getRole()).isEqualTo(RoleName.BACKEND);
        }
    }

    @Test
    @DisplayName("should_GenerateUniqueIdsForUsers_When_ApplicationStarts")
    void should_GenerateUniqueIdsForUsers_When_ApplicationStarts() {
        // Assert
        List<User> users = userRepository.findAll();
        
        // All users should have unique IDs
        long uniqueIdCount = users.stream()
                .map(User::getId)
                .distinct()
                .count();
        
        assertThat(uniqueIdCount).isEqualTo(11);
    }

    @Test
    @DisplayName("should_GenerateUniqueGithubUrls_When_ApplicationStarts")
    void should_GenerateUniqueGithubUrls_When_ApplicationStarts() {
        // Assert
        List<User> users = userRepository.findAll();
        
        // All users should have unique GitHub URLs
        long uniqueUrlCount = users.stream()
                .map(User::getGithubUrl)
                .distinct()
                .count();
        
        assertThat(uniqueUrlCount).isEqualTo(11);
    }
    
    @Test
    @DisplayName("should_SeedRealUser_When_ApplicationStarts")
    void should_SeedRealUser_When_ApplicationStarts() {
        // Assert
        Optional<User> realUser = userRepository.findByGithubUrl("https://github.com/mahmutsyilmz");
        
        assertThat(realUser).isPresent();
        assertThat(realUser.get().getName()).isEqualTo("Mahmut Sami Yılmaz");
        assertThat(realUser.get().getSurname()).isNull();
        assertThat(realUser.get().getRole()).isNull();
        assertThat(realUser.get().getId().toString()).isEqualTo("63357787-77fd-4175-9a41-e899dc4c100e");
    }

    @Test
    @DisplayName("should_AddDummyUsersToWaitingQueue_When_ApplicationStarts")
    void should_AddDummyUsersToWaitingQueue_When_ApplicationStarts() {
        // Assert - All dummy users should have waitingSince set (in queue)
        List<User> dummyUsers = userRepository.findAll().stream()
                .filter(user -> user.getGithubUrl().startsWith("https://github.com/fake_"))
                .collect(Collectors.toList());

        assertThat(dummyUsers).hasSize(10);

        for (User user : dummyUsers) {
            assertThat(user.getWaitingSince())
                    .as("User %s should be in waiting queue", user.getName())
                    .isNotNull();
        }
    }

    @Test
    @DisplayName("should_HaveCorrectQueueOrder_FIFO_When_ApplicationStarts")
    void should_HaveCorrectQueueOrder_FIFO_When_ApplicationStarts() {
        // Assert - Frontend users should have staggered queue times (FE1 oldest, FE5 newest)
        List<User> frontendUsers = userRepository.findAll().stream()
                .filter(user -> user.getRole() == RoleName.FRONTEND)
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .collect(Collectors.toList());

        // Bot Frontend 1 should have joined before Bot Frontend 2, etc.
        for (int i = 0; i < frontendUsers.size() - 1; i++) {
            User earlier = frontendUsers.get(i);
            User later = frontendUsers.get(i + 1);
            
            assertThat(earlier.getWaitingSince())
                    .as("%s should have joined queue before %s", earlier.getName(), later.getName())
                    .isBefore(later.getWaitingSince());
        }
    }

    @Test
    @DisplayName("should_NotAddRealUserToQueue_When_ApplicationStarts")
    void should_NotAddRealUserToQueue_When_ApplicationStarts() {
        // Assert - Real user should NOT be in the queue
        Optional<User> realUser = userRepository.findByGithubUrl("https://github.com/mahmutsyilmz");
        
        assertThat(realUser).isPresent();
        assertThat(realUser.get().getWaitingSince())
                .as("Real user should not be in waiting queue")
                .isNull();
    }

    @Test
    @DisplayName("should_HaveQueryableWaitingUsers_When_ApplicationStarts")
    void should_HaveQueryableWaitingUsers_When_ApplicationStarts() {
        // Assert - Can find oldest waiting user by role
        Optional<User> oldestFrontend = userRepository.findOldestWaitingByRole(
                "FRONTEND", 
                java.util.UUID.randomUUID() // exclude non-existent user
        );
        
        assertThat(oldestFrontend).isPresent();
        assertThat(oldestFrontend.get().getName()).isEqualTo("Bot Frontend 1"); // First in queue
        
        Optional<User> oldestBackend = userRepository.findOldestWaitingByRole(
                "BACKEND", 
                java.util.UUID.randomUUID()
        );
        
        assertThat(oldestBackend).isPresent();
        assertThat(oldestBackend.get().getName()).isEqualTo("Bot Backend 1"); // First in queue
    }
}
