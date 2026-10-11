# Scheduled Attendance Session Calendar Projection (MENT-#88)

## Authoritative source and eligibility

The authoritative source is `AttendanceSession` in Attendance Service. It represents a planned activity with a course, title, date, start time, end time, and lifecycle status. Individual `AttendanceRecord` rows and attendance outcomes are never projected.

Only an active `OPEN` session whose start time is in the future is eligible. Completed, cancelled, removed, inactive, or already-started sessions are not active Calendar projections. A previously projected session is retained as `CANCELLED` when it becomes ineligible through completion, cancellation, or removal.

## Mapping

| AttendanceSession | Calendar event |
|---|---|
| `id` | `sourceType=ATTENDANCE`, `sourceId=id` |
| `title` | `title` |
| `attendanceDate + startTime` | `startDateTime` |
| `attendanceDate + endTime` | `endDateTime` |
| `courseId` | `courseId`, `visibility=COURSE` |
| `createdBy` | `createdBy` |
| no location/description fields | `location=null`, `description=null` |
| `OPEN` and future | active `SCHEDULED` projection |
| completed/cancelled/removed/inactive | retained `CANCELLED` projection |

## Security and lifecycle

Attendance Service calls:

- `POST /api/v1/internal/calendar/attendance-projections`
- `POST /api/v1/internal/calendar/attendance-projections/{sourceId}/cancel`
- `POST /api/v1/internal/calendar-reconciliation/class-sessions/{sessionId}`

Calls require the forwarded user `Authorization` header, `X-Internal-Service-Name: attendance-service`, and the shared `X-Internal-Service-Token`. Calendar Service additionally requires an authorized JWT role. Course visibility is evaluated by Calendar using Enrollment and Course authorization: Students see enrolled courses and Teachers see assigned courses. Client course filters never grant access.

Manual Calendar APIs cannot change `ATTENDANCE` projections. Calendar's `(sourceType, sourceId)` uniqueness and synchronized upsert protect repeated and concurrent delivery. `ATTENDANCE` is independent from `COURSE_CLASS`, `EXAM`, `ASSIGNMENT`, and `QUIZ` source types.

## Failure and recovery

Attendance session operations remain successful when Calendar is unavailable. Attendance Service records success/failure state, attempt count, source ID, and safe exception type in `calendar_projection_attempts`. Reconciliation reloads the authoritative session and either upserts the current future `OPEN` state or cancels the retained projection.

Calendar validates required IDs, course, title, creator, and strictly increasing times before persistence. Structured errors and safe logs never expose service tokens, credentials, raw exception messages, or unnecessary personal data.
