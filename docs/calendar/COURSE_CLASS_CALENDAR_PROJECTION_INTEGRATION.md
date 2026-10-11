# Course Class Schedule Calendar Projection (MENT-#87)

## Authoritative source

The repository does not contain a separate Course class-schedule entity. Course Service owns course metadata and teacher assignment, but the existing schedule-like record is `AttendanceSession` in Attendance Service. Therefore Attendance Service is the authoritative source for this integration.

`AttendanceSession` supplies the schedule ID, course ID, title, attendance date, start/end time, and lifecycle status. Calendar Service stores only a projection and does not create or own attendance sessions.

## Mapping

| AttendanceSession | Calendar event |
|---|---|
| `id` | `sourceType=COURSE_CLASS`, `sourceId=id` |
| `title` | `title` |
| `attendanceDate + startTime` | `startDateTime` |
| `attendanceDate + endTime` | `endDateTime` |
| `courseId` | `courseId`, `visibility=COURSE` |
| `createdBy` | `createdBy` |
| no location field exists | `location=null` |
| no description field exists | `description=null` |
| `OPEN` or `COMPLETED` | active `SCHEDULED` Calendar projection |
| `CANCELLED` or inactive source | retained `CANCELLED` Calendar projection |

## Lifecycle and visibility

- Creating an `OPEN` session creates one Calendar projection.
- Completing a session refreshes its source projection; it remains retained as a scheduled class event for historical/calendar consistency.
- Cancelling a session cancels the retained Calendar event without hard deletion.
- Repeated delivery is idempotent. Calendar's `(sourceType, sourceId)` unique constraint prevents duplicates, including concurrent delivery. `COURSE_CLASS` is independent from `EXAM`, `ASSIGNMENT`, and `QUIZ` even when UUID values match.
- Calendar query authorization remains authoritative: Students see only enrolled courses and Teachers see only assigned courses. A client-supplied course filter never grants access.
- Manual Calendar APIs cannot modify or cancel a `COURSE_CLASS` projection.

## Internal API and recovery

Attendance Service calls:

- `POST /api/v1/internal/calendar/class-projections`
- `POST /api/v1/internal/calendar/class-projections/{sourceId}/cancel`
- `POST /api/v1/internal/calendar-reconciliation/class-sessions/{sessionId}`

Projection calls require the forwarded `Authorization` header, `X-Internal-Service-Name: attendance-service`, and the shared `X-Internal-Service-Token`. `CALENDAR_INTERNAL_SERVICE_TOKEN` must be identical in Attendance Service and Calendar Service environments.

Calendar failures are isolated from Attendance Service operations. Failed and successful outcomes are recorded in Attendance Service's `calendar_projection_attempts` table with source identity, state, count, and safe exception type. Reconciliation reads the current AttendanceSession and converges Calendar without creating duplicates.

Invalid IDs, titles, courses, creators, or date ranges are rejected by Calendar's structured validation/error handling before persistence. Logs contain only source identifiers and dependency status/type; service tokens and raw exception messages are not logged or persisted.
