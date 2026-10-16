# backend
Mentify backend monorepo built with six Spring Boot microservices (User, Course, Assessment, Communication, Analytics, AI) using microservices architecture. Implements JWT authentication, RBAC, Apache Kafka event-driven communication, OpenAI-powered RAG chatbots and AI grading, with PostgreSQL (AWS RDS) and MongoDB Atlas databases.

## Read Flow Contract (MENT-15)

This contract standardizes read operations for:
- Lessons
- Modules
- Lesson Materials

### Endpoint Availability

Each entity supports:
- Single record retrieval: `GET /api/v1/{entity}/{id}`
- List retrieval: `GET /api/v1/{entity}`

Entities map as:
- `lessons`
- `modules`
- `lesson-materials`

### Standard Response Envelope and DTO Rules

All successful responses must use the same envelope shape:

```json
{
  "success": true,
  "message": "Request successful",
  "data": {}
}
```

Single retrieval (`GET /api/v1/{entity}/{id}`):
- `data` is a single DTO object for the requested entity.

List retrieval (`GET /api/v1/{entity}`):
- `data` contains an items list and pagination metadata:
## Course Content Create Contract (MENT-#13)

This ticket defines the initial **create** contract for:
- Modules
- Lessons
- Lesson Materials

### Create Endpoint Availability

Expose add/create operations under the course content API:
- `POST /courses/{courseId}/modules`
- `POST /modules/{moduleId}/lessons`
- `POST /lessons/{lessonId}/materials`

### Validation and Safety Rules

Each create request must enforce required fields with Jakarta Bean Validation (`@Valid`) and reject malformed input with standard API error responses.

Minimum validation expectations:
- **Module**: `title` required
- **Lesson**: `title` required
- **Lesson Material**: `type` and `url` required

Duplicate content names within the same parent scope must be rejected with a conflict-style response.

### Service Layer Contract

Controllers delegate to service methods, and services persist entities through repository interfaces using the standard Spring Boot application flow (controller -> service -> repository).

### Standard Response Shape

Successful create operations must return the shared success envelope:

```json
{
  "success": true,
  "message": "Request successful",
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

### Search, Filtering, and Pagination

List endpoints accept optional query parameters:
- `page` (default: `0`)
- `size` (default: `20`)
- `sort` (optional, format: `field,asc|desc`)
- `search` (optional free-text search)
- entity-specific filters (for example `moduleId`, `lessonId`, `status`) where applicable

Behavior:
- Invalid pagination values return a validation error.
- Missing optional filters do not restrict results.
- Filtering and paging are applied in the repository/service layer.

### Empty and Not-Found Handling

```txt
docs/PROJECT_SETUP.md   Full local project setup
docs/KEYCLOAK_SETUP.md  Keycloak realm, clients, roles, and token testing
docs/AUTHENTICATION_SESSION_FLOW.md  Login, refresh, logout, and forgot password flows
docs/NOTIFICATION_SERVICE.md  Kafka event notifications, in-app inbox, and SMTP setup
docs/testing/README.md  Testing standards, coverage commands, and registers
CHANGELOG.md            Unreleased changes
```

## Quick Start

```bash
cp .env.example .env
docker compose up -d
```

Keycloak runs at:

```txt
http://localhost:8180
```

For full setup, read [Project Setup](docs/PROJECT_SETUP.md). Notification Service's Kafka and
SMTP/Mailpit configuration is documented in [Notification Service](docs/NOTIFICATION_SERVICE.md).
