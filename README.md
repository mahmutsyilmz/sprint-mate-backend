# Sprint Mate Backend

A Spring Boot backend for matching frontend and backend developers for collaborative projects.

## 🚀 Features

### ✅ Implemented
- **GitHub OAuth2 Authentication** - Login via GitHub
- **User Registration** - Automatic user sync on first login
- **Role Selection API** - Users can select FRONTEND or BACKEND role
- **User Skills/Tech Stack** - Store and update user skills (e.g., "Java", "React", "Docker")
- **Project Templates API** - Browse available collaborative projects
- **FIFO Matching Queue** - First-in-first-out matching system
  - Users join a waiting queue when no partner is available
  - Oldest waiting user gets matched first
  - Cancel waiting feature
- **Match Creation** - Automatic match with project assignment
- **Match Completion** - Complete active matches with optional repo URL
  - Security check ensures only participants can complete
  - Users freed to search for new matches after completion
- **Swagger UI** - Interactive API documentation
- **File-based H2 Database** - Data persists between restarts
- **AI Project Generator Placeholder** - Architecture ready for AI integration

### 📋 Pending
- AI-driven project generation (OpenAI/Gemini integration)
- Real Google Meet integration
- Notification system

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 17 | Language |
| Spring Boot 3.2 | Framework |
| Spring Security | OAuth2 Authentication |
| Spring Data JPA | Data persistence |
| H2 Database | Development database |
| Lombok | Boilerplate reduction |
| SpringDoc OpenAPI | Swagger UI |
| JUnit 5 + Mockito | Testing |
| spring-dotenv | .env file support |

## 📁 Project Structure

```
src/main/java/com/sprintmate/
├── config/          # Security, OpenAPI, DataInitializer
├── constant/        # Application constants
├── controller/      # REST API endpoints
│   ├── UserController.java
│   ├── ProjectController.java
│   └── MatchController.java
├── dto/             # Request/Response DTOs
│   ├── UserResponse.java          # Includes skills field
│   ├── UserUpdateRequest.java     # Includes skills field
│   ├── ProjectTemplateResponse.java
│   ├── MatchStatusResponse.java
│   ├── MatchCompletionRequest.java
│   ├── MatchCompletionResponse.java
│   └── ...
├── exception/       # Custom exceptions & global handler
├── mapper/          # Entity ↔ DTO mappers
├── model/           # JPA entities
│   ├── User.java              # Includes skills (ElementCollection)
│   ├── Match.java
│   ├── MatchParticipant.java
│   ├── MatchProject.java
│   └── ProjectTemplate.java
├── repository/      # Data access layer
└── service/         # Business logic
    ├── UserService.java
    ├── ProjectService.java
    ├── MatchService.java
    ├── ProjectGeneratorService.java  # AI project generation interface
    └── AiProjectGenerator.java       # AI integration placeholder
```

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- GitHub OAuth App credentials

### Setup

1. **Clone the repository**
```bash
git clone <repository-url>
cd sprint-mate-backend
```

2. **Create `.env` file** in project root:
```properties
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
```

3. **Run the application**
```bash
# Windows
.\run.bat

# Or manually with PowerShell
$env:GITHUB_CLIENT_ID="your-id"
$env:GITHUB_CLIENT_SECRET="your-secret"
mvn spring-boot:run
```

4. **Access the application**
- Application: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

## 📡 API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/oauth2/authorization/github` | Initiate GitHub login |

### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/me` | Get current user profile (includes skills) |
| PUT | `/api/users/me` | Update user profile (name, bio, role, skills) |
| PATCH | `/api/users/me/role` | Update user role (FRONTEND/BACKEND) |

### Projects
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/projects` | Get all project templates |

### Matches
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/matches/find` | Find match or join queue |
| DELETE | `/api/matches/queue` | Leave the waiting queue |
|| POST | `/api/matches/{matchId}/complete` | Complete an active match |

## 🎯 Matching Algorithm (FIFO Queue)

```
┌─────────────────────────────────────────────────────────────┐
│                    MATCHING FLOW                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  User calls POST /api/matches/find                          │
│                    │                                        │
│                    ▼                                        │
│  ┌─────────────────────────────────┐                       │
│  │ Is there a waiting partner      │                       │
│  │ with opposite role?             │                       │
│  └─────────────────────────────────┘                       │
│           │                │                                │
│          YES              NO                                │
│           │                │                                │
│           ▼                ▼                                │
│  ┌─────────────┐  ┌─────────────────────┐                  │
│  │ MATCHED!    │  │ Join waiting queue  │                  │
│  │             │  │ (set waitingSince)  │                  │
│  │ - Match     │  │                     │                  │
│  │ - Project   │  │ Return: WAITING     │                  │
│  │ - Meet URL  │  │ with queue position │                  │
│  └─────────────┘  └─────────────────────┘                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Queue Order: FIFO (First In, First Out)
- Oldest waiting user gets matched first
- waitingSince timestamp determines order
```

### Example Flow
```
1. Frontend Dev A joins → No Backend waiting → A joins queue (WAITING)
2. Frontend Dev B joins → No Backend waiting → B joins queue (WAITING)
3. Backend Dev X joins → Frontend A is oldest → Match: X ↔ A (MATCHED)
4. Frontend Dev C joins → No Backend waiting → C joins queue (WAITING)
5. Backend Dev Y joins → Frontend B is oldest → Match: Y ↔ B (MATCHED)
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MatchServiceTest

# Run with coverage
mvn test jacoco:report
```

### Test Summary
- **Unit tests**: `UserServiceTest`, `ProjectServiceTest`, `MatchServiceTest`
- **Integration tests**: `UserControllerTest`, `ProjectControllerTest`, `MatchControllerTest`
- **Data tests**: `DataInitializerTest`

## 📝 Configuration

### Environment Variables
| Variable | Description | Required |
|----------|-------------|----------|
| `GITHUB_CLIENT_ID` | GitHub OAuth App Client ID | Yes |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth App Client Secret | Yes |

### GitHub OAuth App Setup
1. Go to GitHub → Settings → Developer settings → OAuth Apps
2. Create new OAuth App
3. Set Homepage URL: `http://localhost:8080`
4. Set Callback URL: `http://localhost:8080/login/oauth2/code/github`

## 🗃️ Database Schema

```
┌─────────────┐     ┌─────────────────────┐     ┌─────────────────┐
│   users     │     │ match_participants  │     │    matches      │
├─────────────┤     ├─────────────────────┤     ├─────────────────┤
│ id (PK)     │◄────│ user_id (FK)        │     │ id (PK)         │
│ name        │     │ match_id (FK)       │────►│ status          │
│ surname     │     │ participant_role    │     │ communication_  │
│ github_url  │     └─────────────────────┘     │   link          │
│ role        │                                 │ created_at      │
│ bio         │     ┌─────────────────────┐     │ expires_at      │
│ waiting_    │     │   match_projects    │     └─────────────────┘
│   since     │     ├─────────────────────┤            ▲
└──────┬──────┘     │ match_id (FK)       │────────────┤
       │            │ project_template_   │            │
       │            │   id (FK)           │────►┌──────┴──────────┐
       ▼            │ start_date          │     │project_templates│
┌─────────────┐     │ end_date            │     ├─────────────────┤
│ user_skills │     └─────────────────────┘     │ id (PK)         │
├─────────────┤                                 │ title           │
│ user_id(FK) │     ┌─────────────────────┐     │ description     │
│ skill       │     │ match_completions   │     └─────────────────┘
└─────────────┘     ├─────────────────────┤
                    │ id (PK)             │
                    │ match_id (FK)       │─────► (references matches.id)
                    │ completed_at        │
                    │ repo_url            │
                    └─────────────────────┘
```

## 🤖 AI Project Generation (Planned)

The `AiProjectGenerator` service is a placeholder for future AI integration:

```
┌─────────────────────────────────────────────────────────────┐
│                 AI PROJECT GENERATION FLOW                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Frontend User Skills: [React, TypeScript, Tailwind, Vite]  │
│  Backend User Skills:  [Java, Spring Boot, PostgreSQL]      │
│                    │                                         │
│                    ▼                                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Construct AI Prompt:                                  │   │
│  │ "Create a 1-week project for these skills..."        │   │
│  └──────────────────────────────────────────────────────┘   │
│                    │                                         │
│                    ▼                                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Call OpenAI/Gemini API                               │   │
│  └──────────────────────────────────────────────────────┘   │
│                    │                                         │
│                    ▼                                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Parse JSON → ProjectTemplate                         │   │
│  │ {title, description, frontendTasks, backendTasks}    │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Seeded Skills for Testing
- **Frontend users**: React, TypeScript, Tailwind, Vite
- **Backend users**: Java, Spring Boot, PostgreSQL, Docker

## 📄 License

This project is for educational purposes.

---

**Last Updated:** 2026-01-11
