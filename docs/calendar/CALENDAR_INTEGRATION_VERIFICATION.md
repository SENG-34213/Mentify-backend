# Calendar Integration Verification (MENT-#90)

## Verification scope

Calendar Service is a projection/query service. The authoritative records remain in Exam, Assignment/Quiz, Attendance, Course, Enrollment, and Communication services. This document records the repeatable local verification boundary for the Calendar backend and the checks that require running service infrastructure or staging.

## Supported source matrix

| Source | Calendar source type | Current source authority | Local verification |
|---|---|---|---|
| Scheduled Exam | `EXAM` | Exam Service `Exam` | Projection, cancellation, retry attempt, and reconciliation tests |
| Published Assignment | `ASSIGNMENT` | Assignment Service `Assignment` | Projection, lifecycle, retry attempt, and reconciliation tests |
| Published Quiz | `QUIZ` | Assignment Service `Quiz` | Projection, lifecycle, retry attempt, and reconciliation tests |
| Planned attendance/class session | `ATTENDANCE` | Attendance Service `AttendanceSession` | Eligibility, cancellation, retry attempt, and reconciliation tests |
| Communication message/group | None currently | Communication Service | Confirmed unsupported; ordinary messages and timestamps are excluded |

## Authorization and query contract

Calendar queries derive caller identity and role from the JWT. Students are authorized through current Enrollment Service membership; teachers are authorized through current Course Service assignment. Dependency failures fail closed with a structured `503 Service Unavailable`; Calendar never returns unverified course events. Administrators follow the documented administrative visibility policy.

Range queries use the documented half-open overlap rule, deterministic `startDateTime`/ID ordering, a maximum range, and bounded upcoming limits. Private events are visible only to their owner. Manual APIs cannot mutate source-managed projections.

## Failure and recovery contract

Each source service persists its authoritative change before attempting Calendar projection. A Calendar/Feign failure is caught after the source save, recorded in that source service's projection-attempt table using a safe exception type, and does not roll back the source operation. Reconciliation reloads the current authoritative source and sends an idempotent upsert or cancellation to Calendar. Calendar source uniqueness `(sourceType, sourceId)` prevents duplicates during repeated delivery.

Current reconciliation entry points are:

- Exam Service: `POST /api/v1/internal/exams/{examId}/calendar-reconcile`
- Assignment Service: `POST /api/v1/internal/calendar-reconciliation/assignments/{assignmentId}`
- Assignment Service: `POST /api/v1/internal/calendar-reconciliation/quizzes/{quizId}`
- Attendance Service: `POST /api/v1/internal/calendar-reconciliation/class-sessions/{sessionId}`

These endpoints require established internal service authorization. Tokens are never written to logs or returned in error responses.

## Running the local verification

```text
mvn -s maven-verification-settings.xml -pl services/exam-service,services/assignment-service,services/attendance-service,services/calender-service -am test
```

The settings file is temporary and must not be committed. The run covers Calendar domain/repository/API/query tests and source-service projection, failure-isolation, cancellation, idempotency, and reconciliation tests.

In the current local environment, the aggregate run is blocked during Attendance Service test compilation by the cached `jackson-datatype-jdk8:2.15.4` artifact in the user's Maven repository. This is an environment/toolchain issue, not an assertion failure. Refreshing that dependency in the developer's Maven cache or CI is required before claiming the complete aggregate run passed.

## Environment-dependent checks

Full HTTP flows against Eureka, Config Server, Keycloak, Course Service, Enrollment Service, PostgreSQL, and staging deployment are not reproducible in the isolated local test profile. They must be executed in integration/staging and recorded as `BLOCKED` until run; they must not be reported as passed based on unit tests.

