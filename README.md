# Sprint Mate Backend

A Spring Boot backend for matching frontend and backend developers for collaborative projects.

## 🚀 Features

### ✅ Implemented
- **GitHub OAuth2 Authentication** - Login via GitHub
- **User Registration** - Automatic user sync on first login
- **Role Selection API** - Users can select FRONTEND or BACKEND role
- **Swagger UI** - Interactive API documentation
- **File-based H2 Database** - Data persists between restarts

### 📋 Pending
- Match creation and management
- Project assignment
- Match completion flow

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

## 📁 Project Structure

```
src/main/java/com/sprintmate/
├── config/          # Security, OpenAPI configuration
├── constant/        # Application constants
├── controller/      # REST API endpoints
├── dto/             # Request/Response DTOs
├── exception/       # Custom exceptions & global handler
├── mapper/          # Entity ↔ DTO mappers
├── model/           # JPA entities
├── repository/      # Data access layer
└── service/         # Business logic
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
| GET | `/api/users/me` | Get current user profile |
| PATCH | `/api/users/me/role` | Update user role (FRONTEND/BACKEND) |

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

### Test Summary
- **21 tests** total
- Unit tests: `UserServiceTest` (10 tests)
- Integration tests: `UserControllerTest` (10 tests)
- Application context test (1 test)

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

## 📄 License

This project is for educational purposes.

---

**Last Updated:** 2026-01-11
