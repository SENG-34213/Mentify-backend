# Exam-to-Calendar Projection (MENT-#85)

## Mapping and eligibility

Exam Service remains authoritative. Only exams in `SCHEDULED` state are projected; `DRAFT` exams are not visible. Later `MARKING` and `COMPLETED` transitions do not remove the scheduled calendar event. `CANCELLED` exams retain their projection with Calendar status `CANCELLED`.

| Exam | Calendar |
|---|---|
| `id` | `sourceId` |
| fixed source | `sourceType=EXAM` |
| `title` | `title` |
| `description` | `description` |
| `examDate + startTime` | `startDateTime` |
| `examDate + endTime` | `endDateTime` |
| `courseId` | `courseId` |
| `createdBy` | projection audit owner |
| no source location exists | `location=null` |
| approved visibility | `visibility=COURSE` |

## Internal operations

Exam Service calls Calendar Service through the protected internal endpoints:

- `POST /api/v1/internal/calendar/exam-projections`
- `POST /api/v1/internal/calendar/exam-projections/{examId}/cancel`

Calls require a valid JWT with an allowed service caller role plus `X-Internal-Service-Name=exam-service` and the configured `X-Internal-Service-Token`. The token is supplied through `CALENDAR_INTERNAL_SERVICE_TOKEN`; it is never committed or returned. These endpoints are not frontend manual-event APIs.

## Idempotency and failure recovery

The `(sourceType, sourceId)` database uniqueness constraint identifies one Exam projection. Upsert compares the current mapped values before saving, so repeated unchanged deliveries do not update the record. Cancellation is retained and repeated cancellation is a no-op. The Calendar projection service is synchronized in-process and the database constraint remains the final duplicate-prevention guard.

Exam changes are saved before projection failure is handled. Calendar Feign failures are logged with the Exam UUID only and persisted in `exam_calendar_projection_attempts`; they do not roll back the successful Exam operation. An administrator can reconcile the authoritative current state through the direct-service operation `POST /api/v1/internal/exams/{examId}/calendar-reconcile` after the dependency recovers. Reconciliation is itself idempotent.

Projected events use the existing Calendar course visibility policy, so Students require Enrollment Service authorization and Teachers require Course Service assignment when querying calendar feeds. Manual Calendar update/cancel operations continue to reject `EXAM` source events.
