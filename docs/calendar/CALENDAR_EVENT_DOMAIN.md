# Calendar Event Domain and Persistence

Ticket: MENT-#82  
Service: `calender-service`

## Scope

This ticket provides the calendar event domain model, Flyway schema, repository queries, and persistence constraints. Public calendar APIs, projection consumers, reminders, recurrence, and Google Calendar synchronization are intentionally outside this ticket.

## Event contract

`CalendarEvent` stores UUID identity, title (required, 200 characters maximum), optional description, event type, start/end timestamps, all-day flag, optional course and source references, creator identity, visibility, optional target role, lifecycle status, optional location (255 characters maximum), and immutable creation plus mutable update timestamps.

Supported event types are `CLASS`, `ASSIGNMENT`, `QUIZ`, `EXAM`, `MEETING`, `HOLIDAY`, `ADMIN`, `CUSTOM`, and `ATTENDANCE`. Supported source types are `EXAM`, `ASSIGNMENT`, `QUIZ`, `COURSE_CLASS`, `ATTENDANCE`, `COMMUNICATION`, `CUSTOM`, and `ADMIN`.

## Business rules

- Timed events require an end timestamp; all-day events may omit it.
- An end timestamp cannot precede the start timestamp.
- `COURSE` visibility requires `courseId`; `ROLE` visibility requires `targetRole` (`STUDENT`, `TEACHER`, `ADMIN`, or `SUPER_ADMIN`).
- External source types require `sourceId`. `CUSTOM` and `ADMIN` are manual source types and may have a null source ID.
- `(sourceType, sourceId)` is unique at the database level. PostgreSQL/H2 null semantics intentionally allow multiple manual events with null source IDs.
- Cancellation updates the existing row to `CANCELLED`; the event ID, creator, and source references remain unchanged.
- `createdBy` is supplied by the trusted caller layer and is never overwritten by updates.

## Time and query conventions

The service persists `LocalDateTime` values without a database time-zone shift. Audit timestamps use `Asia/Colombo`, the application time zone. Callers are responsible for converting external instants into this convention before persistence.

`findOverlapping` uses inclusive boundaries: an event overlaps when `startDateTime <= rangeEnd` and its effective end (`endDateTime`, or `startDateTime` for an all-day event) is `>= rangeStart`. Results are ordered by start time and UUID for deterministic chronology. Optional course, creator, type, visibility, source type, and status filters are applied in the same repository query.

Schema constraints and repository tests are the final enforcement layer for concurrent writes and unsupported persisted enum values; bean validation provides the fast application-layer feedback.
