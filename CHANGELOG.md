# Changelog

## [Unreleased]

- Added `ExamResult` persistence and student-eligibility initialization (MENT-#41): `exam_results` table (Flyway `V3`, `UNIQUE (exam_id, student_id)`, FK to `exams`, indexes on `exam_id` and `student_id`), `ExamResult` entity with `AttendanceStatus` and `ResultStatus` enums, `ExamResultRepository`, and `ExamResultService.initializeResults(examId, authorizationHeader)`. Eligible students come from Enrollment Service (`GET /api/v1/enrollments/courses/{courseId}/students`, caller token forwarded); one `NOT_MARKED` result with null marks/attendance/marker is created per student without a result, so repeated runs are idempotent and an empty roster creates nothing. Cancelled exams return 409; Enrollment failures fail closed with 503. No local Student/Enrollment data is stored. No REST endpoint is exposed yet.

- Added exam management APIs (MENT-#40) under `/api/v1/exams`: `POST /` (create, status DRAFT, `createdBy` from JWT), `GET /{examId}`, `GET /courses/{courseId}`, `PUT /{examId}` (DRAFT/SCHEDULED only), `POST /{examId}/cancel` (DRAFT/SCHEDULED only). Responses use `ApiResponse<ExamResponse>`; lifecycle violations return 409. SUPER_ADMIN/ADMIN have full access, TEACHER only for courses assigned to them (verified via Course Service Feign lookup, fails closed with 503 when unavailable), STUDENT is denied (403). Marks/time rules validated by bean validation and the service layer.

- Added the physical Exam persistence model (#39): UUID-backed Exam entity, type/status enums, Flyway schema with integrity constraints and course/date indexes, repository query methods, and persistence tests.
- Added exam-service foundation (#38): Keycloak JWT security via common-lib, JWT-derived user identity/roles, safe global exception handling, health checks, env-based config in config-server (exam-service.yml, port 8089), gateway route /api/v1/exams/**, CI/docker/env wiring. Schema is managed by Flyway (db/migration, same setup as user-service) with Hibernate ddl-auto: validate.
- Added the user-service Keycloak login bridge at `POST /api/v1/auth/login`.
- Added local profile status and role consistency checks after successful Keycloak authentication.
- Added first-login activation for complete local `INVITED` profiles after successful Keycloak authentication.
- Added safe authentication event logging and controlled authentication error responses.
- Documented login bridge configuration, behavior, troubleshooting, and refresh-token policy boundary.
- Added Keycloak-native forgot password request flow at `POST /api/v1/auth/forgot-password`.
- Added email-enumeration-safe forgot password responses and safe reset event logging.
