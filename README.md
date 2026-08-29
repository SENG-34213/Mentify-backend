# backend
Mentify backend monorepo built with six Spring Boot microservices (User, Course, Assessment, Communication, Analytics, AI) using microservices architecture. Implements JWT authentication, RBAC, Apache Kafka event-driven communication, OpenAI-powered RAG chatbots and AI grading, with PostgreSQL (AWS RDS) and MongoDB Atlas databases.

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
  "message": "Created successfully",
  "data": {
    "id": "..."
  }
}
```

Validation and duplicate errors must use the shared global exception handling format.
