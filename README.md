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

Single retrieval (`GET /{entity}/{id}`):
- `data` is a single DTO object for the requested entity.

List retrieval (`GET /{entity}`):
- `data` contains an items list and pagination metadata:

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

- Single retrieval with unknown `{id}` returns a consistent not-found response:
  - HTTP `404`
  - `success: false`
  - message indicating entity not found
- List retrieval with no matches returns:
  - HTTP `200`
  - `success: true`
  - empty `items` with valid pagination metadata

### Implementation Notes

- Keep retrieval logic isolated in repository and service layers.
- Reuse shared DTO and exception conventions.
- Do not expose environment credentials or internal secrets in any response payload.
