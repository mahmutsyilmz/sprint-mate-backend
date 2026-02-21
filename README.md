# Sprint Mate Backend

A Spring Boot backend for matching frontend and backend developers to build AI-generated collaborative sprint projects.

**Last Updated:** 2026-02-21

---

## Features

### Implemented
- **GitHub OAuth2 Authentication** — Session-cookie based login (JSESSIONID), zero-friction onboarding
- **User Profile Management** — Name, bio, skills (ElementCollection), automatic GitHub sync on login
- **Role Selection** — FRONTEND or BACKEND role assignment
- **Project Preferences** — Theme, difficulty, and learning goal preferences per user
- **FIFO Matching Queue** — First-in-first-out developer pairing by opposite role
  - Oldest waiting user is always matched first (`waitingSince` timestamp)
  - Cancel waiting at any time
- **AI Project Generation** — Groq API (Llama-3.3-70b-versatile)
  - Modular prompt assembly: archetype + theme + user skills + learning goals + crisis scenario
  - Smart archetype & theme selection from intersected user preferences
  - Spring Retry with exponential backoff on rate limit (HTTP 429)
  - Fallback template on generation failure
- **Real-time WebSocket Chat** — STOMP protocol over WebSocket (`/ws`), messages persisted to DB
- **Sprint Completion** — Participants mark sprint done with optional repository URL
- **AI Sprint Review** — Groq fetches GitHub README and scores project 0–100 with strengths/missing elements
- **Session Persistence** — `/api/users/me/status` restores active match state on page refresh
- **API Documentation** — Swagger UI (development profile only)
- **Rate Limiting** — `RateLimitFilter` protects AI endpoints
- **Database Migrations** — Flyway (MSSQL-compatible SQL)
- **Code Coverage** — JaCoCo with 50% minimum line threshold (build fails if not met)

### Pending
- Notification system
- Google Meet / calendar integration
- Advanced skill-based matching algorithm

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
| Spring Boot | 3.2.1 | Framework |
| Spring Security + OAuth2 Client | — | GitHub OAuth2 authentication |
| Spring Data JPA + Hibernate | — | ORM / persistence |
| Microsoft SQL Server | — | Production database |
| H2 | — | Development / test database |
| Flyway | — | Database migrations |
| Spring WebSocket + STOMP | — | Real-time chat |
| Groq API (Llama-3.3-70b-versatile) | — | AI project generation & sprint review |
| Spring Retry + Spring AOP | — | Retry on rate limit (429) |
| SpringDoc OpenAPI | 2.3.0 | Swagger UI |
| Lombok | — | Boilerplate reduction |
| JaCoCo | 0.8.11 | Code coverage (50% minimum) |
| spring-dotenv | 4.0.0 | `.env` file support |
| JUnit 5 + Mockito + AssertJ | — | Unit testing |

---

## Project Structure

```
src/main/java/com/sprintmate/
├── SprintMateApplication.java
├── config/
│   ├── SecurityConfig.java             # CORS, OAuth2, session management
│   ├── WebSocketConfig.java            # STOMP endpoint registration (/ws, /ws-sockjs)
│   ├── WebSocketSecurityConfig.java    # WebSocket session authentication
│   ├── OpenApiConfig.java              # Swagger/SpringDoc configuration
│   ├── RestClientConfig.java           # REST client for Groq/GitHub APIs
│   ├── RateLimitFilter.java            # API rate limiting
│   ├── DataInitializer.java            # Seed data on startup
│   ├── ProjectIdeaInitializer.java     # Project ideas seed data
│   └── ArchetypeThemeInitializer.java  # Archetypes & themes seed data
├── constant/
│   └── GitHubConstants.java           # OAuth scopes, API endpoints
├── controller/
│   ├── UserController.java             # /api/users/**
│   ├── MatchController.java            # /api/matches/**
│   ├── ProjectController.java          # /api/projects/**
│   └── ChatController.java             # /api/chat/**, /app/chat.send (WS)
├── dto/
│   ├── UserResponse.java
│   ├── UserStatusResponse.java         # User + active match combined
│   ├── UserUpdateRequest.java
│   ├── UserPreferenceRequest.java
│   ├── UserPreferenceResponse.java
│   ├── RoleSelectionRequest.java
│   ├── MatchStatusResponse.java        # MATCHED / WAITING response
│   ├── MatchResponse.java
│   ├── MatchCompletionRequest.java
│   ├── MatchCompletionResponse.java    # Includes AI review score
│   ├── ProjectTemplateResponse.java
│   ├── ProjectThemeResponse.java
│   ├── ChatMessageRequest.java
│   └── ChatMessageResponse.java
├── entity/
│   ├── User.java                       # UUID PK, skills (ElementCollection)
│   ├── Match.java                      # UUID PK, status, created_at
│   ├── MatchParticipant.java           # User ↔ Match join
│   ├── MatchProject.java               # Match ↔ ProjectTemplate join
│   ├── MatchCompletion.java            # Completion record with repo URL
│   ├── ProjectTemplate.java            # AI-generated project spec
│   ├── ProjectTheme.java               # Project theme (domain context)
│   ├── ProjectArchetype.java           # Project archetype (structure pattern)
│   ├── ProjectIdea.java                # Project idea pool
│   ├── ProjectPromptContext.java       # Crisis scenario for AI prompt
│   ├── SprintReview.java               # AI code review result (score 0-100)
│   ├── UserPreference.java             # Theme, difficulty, learning goal
│   ├── ChatMessage.java                # Persisted chat messages
│   ├── RoleName.java                   # Enum: FRONTEND, BACKEND
│   ├── MatchStatus.java                # Enum: CREATED, ACTIVE, COMPLETED
│   └── ParticipantRole.java            # Enum: FRONTEND, BACKEND
├── exception/
│   ├── GlobalExceptionHandler.java     # @ControllerAdvice — standard ApiError format
│   ├── ResourceNotFoundException.java  # 404
│   ├── RoleNotSelectedException.java   # 400 — user hasn't selected role
│   ├── ActiveMatchExistsException.java # 409 — user already in active match
│   ├── InvalidRoleException.java       # 400 — invalid role name
│   ├── NoPartnerAvailableException.java # 200 WAITING — no partner found
│   └── ReadmeNotFoundException.java   # 404 — GitHub README not found
├── mapper/
│   ├── UserMapper.java
│   ├── MatchMapper.java
│   ├── ProjectMapper.java
│   └── ChatMapper.java
├── repository/
│   ├── UserRepository.java
│   ├── MatchRepository.java
│   ├── MatchParticipantRepository.java
│   ├── MatchProjectRepository.java
│   ├── MatchCompletionRepository.java
│   ├── ProjectTemplateRepository.java
│   ├── ProjectThemeRepository.java
│   ├── ProjectArchetypeRepository.java
│   ├── ProjectIdeaRepository.java
│   ├── ProjectPromptContextRepository.java
│   ├── SprintReviewRepository.java
│   ├── UserPreferenceRepository.java
│   └── ChatMessageRepository.java
└── service/
    ├── UserService.java                # Profile management, role updates, status
    ├── MatchService.java               # FIFO matching algorithm, queue, completion
    ├── ProjectService.java             # Project template management
    ├── ProjectGeneratorService.java    # AI generator interface
    ├── GroqProjectGenerator.java       # Groq/Llama implementation with retry
    ├── ProjectSelectionService.java    # Archetype & theme selection from prefs
    ├── ModularPromptBuilder.java       # Prompt assembly for Groq
    ├── ChatService.java                # Message persistence, history
    ├── SprintReviewService.java        # AI code review via GitHub README
    ├── CustomOAuth2UserService.java    # GitHub user upsert on login
    └── GitHubService.java              # GitHub API (README fetch, OAuth token)

src/test/java/com/sprintmate/
├── controller/   UserControllerTest, MatchControllerTest
├── exception/    GlobalExceptionHandlerTest
├── mapper/       UserMapperTest, ProjectMapperTest, ChatMapperTest
├── service/      UserServiceTest, MatchServiceTest, ProjectServiceTest,
│                 ChatServiceTest, GroqProjectGeneratorTest,
│                 ModularPromptBuilderTest, ProjectSelectionServiceTest,
│                 SprintReviewServiceTest, GitHubServiceTest,
│                 CustomOAuth2UserServiceTest
└── util/         TestDataBuilder.java  (shared test fixtures)
```

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- GitHub OAuth App credentials
- Groq API key (https://console.groq.com)
- MSSQL Server (or use H2 for development — zero setup)

### Setup

1. **Clone the repository**
```bash
git clone https://github.com/mahmutsyilmz/sprint-mate-backend.git
cd sprint-mate-backend
```

2. **Create `.env` file** in the project root:
```properties
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
GROQ_API_KEY=your-groq-api-key
GROQ_BASE_URL=https://api.groq.com/openai/v1/chat/completions

# Production MSSQL (optional — H2 is used in dev profile)
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password
```

3. **Run the application**
```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

4. **Access the application**

| URL | Description |
|---|---|
| `http://localhost:8080` | Application root |
| `http://localhost:8080/swagger-ui.html` | Swagger UI (dev only) |
| `http://localhost:8080/h2-console` | H2 Console (dev only) — JDBC: `jdbc:h2:mem:sprintmate` |

### GitHub OAuth App Setup
1. Go to GitHub → Settings → Developer settings → OAuth Apps → New OAuth App
2. Set **Homepage URL**: `http://localhost:8080`
3. Set **Callback URL**: `http://localhost:8080/login/oauth2/code/github`
4. Copy Client ID and Secret to `.env`

---

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|---|---|---|
| GET | `/oauth2/authorization/github` | Initiate GitHub OAuth login |
| GET | `/api/auth/logout` | Logout and invalidate session |

### Users
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/me` | Get current user profile |
| GET | `/api/users/me/status` | Get user status with active match info |
| PATCH | `/api/users/me/role` | Set role (FRONTEND / BACKEND) |
| PUT | `/api/users/me` | Update profile (name, bio, skills) |
| PUT | `/api/users/me/preferences` | Update project preferences |

### Matches
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/matches/find?topic=xxx` | Find match or join FIFO queue |
| DELETE | `/api/matches/queue` | Leave the waiting queue |
| POST | `/api/matches/{matchId}/complete` | Complete a sprint (with optional repo URL) |

### Projects
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/projects` | List project templates |

### Chat
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/chat/history/{matchId}` | Get chat message history |
| WS | `/ws` | WebSocket endpoint (STOMP) |

> Swagger UI with full request/response schemas available at `http://localhost:8080/swagger-ui.html` (development only).

---

## Architecture

```
SYSTEM ARCHITECTURE
====================

   Browser / Frontend (React SPA)
             |
    HTTPS / WebSocket (STOMP)
             |
   +---------+-----------+
   |    Spring Boot       |
   |                      |
   |  SecurityConfig      |      +--------------------------+
   |  (CORS, OAuth2,  )   |      |  Groq API (External)     |
   |   Session)           |      |  Llama-3.3-70b-versatile |
   |  RateLimitFilter     |      |  - AI project generation |
   |                      |      |  - Sprint code review    |
   |  Controllers (4)     |----->+--------------------------+
   |  - UserController    |
   |  - MatchController   |      +--------------------------+
   |  - ProjectController |      |  GitHub API (External)   |
   |  - ChatController    |----->|  - README fetch          |
   |                      |      |  - OAuth2 token exchange |
   |  Services (11)       |      +--------------------------+
   |  - UserService       |
   |  - MatchService      |
   |  - GroqProjectGen.   |
   |  - ProjectSelection  |
   |  - ModularPromptBld. |
   |  - ChatService       |
   |  - SprintReviewSvc.  |
   |  - GitHubService     |
   |  ...                 |
   |                      |
   |  Repositories (13)   |
   |                      |
   +---------+------------+
             |
    Spring Data JPA / Hibernate
             |
   +---------+------------+
   |  Database             |
   |  H2 (dev)             |
   |  MSSQL (production)   |
   |  13+ tables           |
   |  Flyway migrations    |
   +-----------------------+
```

---

## FIFO Matching Flow

```
MATCHING FLOW (FIFO Queue)
===========================

  User calls POST /api/matches/find?topic=xxx
                |
                v
  Does user have an active match?
         |           |
        YES          NO
         |           |
         v           v
  Return existing  Is there a waiting partner
  match            with opposite role?
                     |           |
                    YES          NO
                     |           |
                     v           v
              +-----------+  +-------------------+
              |  MATCHED  |  | Join waiting queue|
              |           |  | (set waitingSince)|
              | 1. Create |  |                   |
              |    Match  |  | Return: WAITING   |
              | 2. Select |  | with queue pos.   |
              |    Project|  +-------------------+
              | 3. Assign |
              |    to both|
              | 4. Clear  |
              |    queues |
              +-----------+
                    |
                    v
  ProjectSelectionService.select(frontendUser, backendUser)
                    |
         +----------+----------+
         |                     |
     Archetype             Theme
     (by target         (intersection
      complexity:        of user prefs
      avg FE+BE)         → union → random)
         |                     |
         +----------+----------+
                    |
         ModularPromptBuilder.buildPrompt()
         [role + skills + archetype + theme
          + learning goals + crisis context
          + output format spec]
                    |
         GroqProjectGenerator.callGroqApi()
         (3x retry, 2x backoff on HTTP 429)
                    |
         ProjectTemplate saved to DB
```

---

## AI Project Generation Flow

```
AI PROJECT GENERATION FLOW
===========================

  Match Created (2 users paired: FE + BE)
                |
                v
  ProjectSelectionService.select(feUser, beUser)
                |
      Load UserPreferences for both users
                |
      Calculate target complexity:
        average(FE complexity + BE complexity)
                |
      Select Archetype:
        filter ProjectArchetypes by complexity range
        pick randomly
                |
      Select Theme:
        intersect FE + BE preferred themes
        if empty → union
        if still empty → pick any active theme
        pick randomly
                |
  ModularPromptBuilder.buildPrompt()
  ┌────────────────────────────────────────┐
  │  Role context (FE/BE skills)           │
  │  + Archetype (structure pattern)       │
  │  + Theme (domain context)              │
  │  + Difficulty & learning goals         │
  │  + Crisis scenario (urgency/context)   │
  │  + Output JSON format spec             │
  └────────────────────────────────────────┘
                |
  GroqProjectGenerator.callGroqApi()
  (Retries 3x on HTTP 429 with 2x exponential backoff)
                |
  Groq returns JSON:
  {
    "title": "...",
    "description": "...",
    "wowFactor": "...",
    "frontendTasks": [...],
    "backendTasks": [...],
    "apiEndpoints": [...]
  }
                |
  ProjectTemplate saved to DB
  (fallback template used on Groq failure)
```

---

## AI Sprint Review Flow

```
AI SPRINT REVIEW FLOW
======================

  User calls POST /api/matches/{matchId}/complete
  Body: { repoUrl: "https://github.com/..." }
                |
                v
  MatchService.completeMatch()
  - Validates user is a match participant
  - Sets match status → COMPLETED
  - Frees both users from queue lock
                |
                v
  SprintReviewService.generateReview(match, repoUrl, token)
                |
         +------+------+
         |             |
  GitHubService      Load ProjectPromptContext
  .fetchReadme()     (crisis scenario for context)
  (authenticated,
   supports private
   repos)
         |             |
         +------+------+
                |
  Build review prompt:
  [crisis context + README content
   + evaluation criteria + JSON output spec]
                |
  callGroqApi() — 3x retry on rate limit
                |
  Parse response:
  {
    "score": 78,
    "feedback": "...",
    "strengths": [...],
    "missingElements": [...]
  }
                |
  SprintReview entity saved to DB
  Score included in MatchCompletionResponse
```

---

## Database Schema

```
DATABASE SCHEMA (13+ tables)
==============================

USERS DOMAIN
─────────────
+------------------+       +-------------------+
|     users        |       |   user_skills     |
+------------------+       +-------------------+
| id        UUID PK|──────>| user_id      FK   |
| github_login     |       | skill             |
| name             |       +-------------------+
| surname          |
| github_url       |       +------------------------------+
| role      ENUM   |       |      user_preferences        |
| bio              |       +------------------------------+
| waiting_since    |──────>| id              UUID PK      |
+------------------+       | user_id         FK UNIQUE    |
                            | difficulty_preference        |
                            | learning_goals               |
                            +------------------------------+
                                          |
                            +------------------------------+
                            |   user_preferred_themes      |  (join table)
                            +------------------------------+
                            | user_preference_id   FK      |
                            | theme_id             FK      |
                            +------------------------------+

MATCH DOMAIN
─────────────
+------------------+       +---------------------------+
|    matches       |       |   match_participants      |
+------------------+       +---------------------------+
| id        UUID PK|──────>| id              UUID PK   |
| status    ENUM   |       | match_id        FK        |
| created_at       |       | user_id         FK        |
| expires_at       |       | participant_role ENUM     |
| communication_   |       +---------------------------+
|   link           |
+------------------+
        |
        |──────────>+---------------------------+
        |           |     match_projects        |
        |           +---------------------------+
        |           | id              UUID PK   |
        |           | match_id        FK        |
        |           | project_template_id FK    |
        |           | archetype_id    FK        |
        |           | theme_id        FK        |
        |           | start_date                |
        |           | end_date                  |
        |           +---------------------------+
        |
        |──────────>+---------------------------+
        |           |    match_completions      |
        |           +---------------------------+
        |           | id              UUID PK   |
        |           | match_id        FK UNIQUE |
        |           | completed_by    FK        |
        |           | completed_at              |
        |           | repo_url                  |
        |           +---------------------------+
        |
        |──────────>+---------------------------+
                    |     sprint_reviews        |
                    +---------------------------+
                    | id              UUID PK   |
                    | match_id        FK UNIQUE |
                    | repo_url                  |
                    | score           0–100     |
                    | ai_feedback     TEXT      |
                    | strengths       JSON      |
                    | missing_elements JSON     |
                    | readme_content  TEXT      |
                    | created_at                |
                    +---------------------------+

PROJECT DOMAIN
───────────────
+---------------------+    +---------------------+
|  project_templates  |    |  project_archetypes |
+---------------------+    +---------------------+
| id        UUID PK   |    | id       UUID PK    |
| title               |    | code     UNIQUE     |
| description         |    | display_name        |
| frontend_tasks TEXT |    | structure_desc TEXT |
| backend_tasks  TEXT |    | component_patterns  |
| difficulty          |    | api_patterns        |
| theme               |    | min_complexity      |
+---------------------+    | max_complexity      |
                            | active              |
+---------------------+    +---------------------+
|  project_themes     |
+---------------------+    +---------------------+
| id       UUID PK    |    | project_prompt_ctx  |
| code     UNIQUE     |    +---------------------+
| display_name        |    | id       UUID PK    |
| domain_context TEXT |    | industry            |
| example_entities    |    | crisis_scenario     |
| active              |    | urgency_level       |
+---------------------+    | ... (20+ fields)    |
                            +---------------------+
+---------------------+
|   project_ideas     |
+---------------------+
| id       UUID PK    |
| name                |
| short_description   |
+---------------------+

CHAT DOMAIN
────────────
+---------------------+
|   chat_messages     |
+---------------------+
| id        UUID PK   |
| match_id  (indexed) |
| sender_id           |
| sender_name         |
| content       TEXT  |
| created_at (indexed)|
+---------------------+
```

---

## Testing

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=MatchServiceTest

# Run with coverage report (output: target/site/jacoco/index.html)
./mvnw verify jacoco:report
```

Coverage threshold: **50% line coverage** enforced by JaCoCo — build fails if not met.

### Test Summary

| Category | Test Classes |
|---|---|
| Service | UserServiceTest, MatchServiceTest, ProjectServiceTest, ChatServiceTest, GroqProjectGeneratorTest, ModularPromptBuilderTest, ProjectSelectionServiceTest, SprintReviewServiceTest, GitHubServiceTest, CustomOAuth2UserServiceTest |
| Controller | UserControllerTest, MatchControllerTest |
| Mapper | UserMapperTest, ProjectMapperTest, ChatMapperTest |
| Exception | GlobalExceptionHandlerTest |
| Utility | TestDataBuilder *(shared fixtures)* |

All tests use JUnit 5 + Mockito + AssertJ. Naming convention: `should_ExpectedBehavior_When_State()`.

---

## Configuration

### Environment Variables

| Variable | Required | Description |
|---|---|---|
| `GITHUB_CLIENT_ID` | Yes | GitHub OAuth App Client ID |
| `GITHUB_CLIENT_SECRET` | Yes | GitHub OAuth App Client Secret |
| `GROQ_API_KEY` | Yes | Groq API key for AI features |
| `GROQ_BASE_URL` | Yes | Groq API endpoint URL |
| `DB_USERNAME` | Production | MSSQL database username |
| `DB_PASSWORD` | Production | MSSQL database password |

### Profiles
- **Default (dev):** H2 in-memory database, Swagger UI enabled, H2 Console enabled
- **Production:** MSSQL, Swagger disabled, secure session cookies

---

## License

Private project — All rights reserved.
