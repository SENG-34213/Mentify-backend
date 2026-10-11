# Assignment and Quiz Calendar Projection (MENT-#86)

Assignment Service remains authoritative. Calendar Service stores only course-scoped projections and never owns Assignment or Quiz records.

## Mapping

| Source | Calendar mapping |
|---|---|
| Assignment `id` | `sourceType=ASSIGNMENT`, `sourceId=id` |
| Assignment `title`, `description` | `title`, `description` |
| Assignment `courseId` | `courseId`, `visibility=COURSE` |
| Assignment `dueDate` | `endDateTime`; `startDateTime=dueDate-1 hour` for the deadline window |
| Assignment `teacherId` | `createdBy` |
| Quiz `id` | `sourceType=QUIZ`, `sourceId=id` |
| Quiz `title`, `description` | `title`, `description` |
| Quiz `courseId` | `courseId`, `visibility=COURSE` |
| Quiz `startTime` | `startDateTime` |
| Quiz `endTime` | `endDateTime`; when absent, `startTime + durationMinutes` |
| Quiz `teacherId` | `createdBy` |

The deadline-only Assignment window is one hour because Calendar timed events require an end time. A deadline-only Quiz uses its configured duration. A published Quiz without `startTime` is not eligible until a valid availability start exists.

## Lifecycle

- `DRAFT` Assignments and Quizzes are not projected.
- `PUBLISHED` Assignments and Quizzes are upserted.
- Assignment `CLOSED`, Assignment deletion, Quiz deletion, and any non-eligible reconciliation state cancel an existing projection while retaining its record.
- Repeated notifications are idempotent. Calendar's `(sourceType, sourceId)` unique constraint prevents duplicates, and source types remain independent even when UUIDs match.
- Manual Calendar event APIs cannot update or cancel these source-managed events.
- Student and Teacher queries continue through Calendar's course/enrollment authorization rules; no source-service records are copied into Calendar.

## Internal operations and recovery

Assignment Service calls:

- `POST /api/v1/internal/calendar/assignment-projections`
- `POST /api/v1/internal/calendar/assignment-projections/{sourceId}/cancel`
- `POST /api/v1/internal/calendar/quiz-projections`
- `POST /api/v1/internal/calendar/quiz-projections/{sourceId}/cancel`

Calls require the caller's `Authorization` header, `X-Internal-Service-Name: assignment-service`, and the shared `X-Internal-Service-Token`. `CALENDAR_INTERNAL_SERVICE_TOKEN` must be configured identically in Assignment and Calendar Service environments.

Source operations catch projection failures after the authoritative save. Failures are recorded in `calendar_projection_attempts` with source type, source ID, state, attempt count, and safe exception type. Administrators can reconcile current source state through:

- `POST /api/v1/internal/calendar-reconciliation/assignments/{assignmentId}`
- `POST /api/v1/internal/calendar-reconciliation/quizzes/{quizId}`

Reconciliation reads the current source record and converges the Calendar projection without creating a duplicate.

## Safe behavior

Projection payload validation rejects missing IDs, course IDs, titles, creators, or invalid date ranges before persistence. Responses use structured `ApiResponse` errors, while logs contain source identifiers and dependency status/type only; tokens and raw exception messages are not emitted or persisted.
