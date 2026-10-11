# Calendar Role-Based Query APIs (MENT-#84)

## Endpoints

`GET /api/v1/calendar/events?start={start}&end={end}&type={type}&courseId={courseId}&status={status}`

`GET /api/v1/calendar/upcoming?limit={limit}`

Both endpoints require a JWT with `SUPER_ADMIN`, `ADMIN`, `TEACHER`, or `STUDENT`. The identity and roles are read from the JWT; request parameters never select the acting user.

## Query rules

- `start` and `end` are required ISO-8601 local date-times and `end` must be after `start`.
- The maximum range is 31 days by default and is configurable with `CALENDAR_QUERY_MAX_RANGE_DAYS`.
- Range matching is half-open: an event starts before `end` and its effective end is after `start`. This includes overlaps, excludes an event beginning exactly at `end`, and excludes an event ending exactly at `start`.
- An all-day event is represented as a point at its start when no end is stored; it matches when that start is inside the requested range.
- Results are ordered by `startDateTime ASC, id ASC`.
- `type`, `courseId`, and `status` are narrowing filters. A course filter outside the caller's authorized scope returns `403` and never expands visibility.
- Cancelled events are excluded by default. Only administrators may request `status=CANCELLED`.
- Upcoming events begin at or after the calendar service clock in `Asia/Colombo`; the default limit is 20 and the maximum is 100. Both limits are configurable.

## Visibility policy

Administrators can query all events permitted by the administrative calendar policy. Teachers see global events, teacher-role events, their assigned-course events, and private events they own. Students see global events, student-role events, their enrolled-course events, and private events they own.

Course assignment and enrollment are checked through the dependent services. A dependency failure fails closed with structured `503 Service Unavailable`; the calendar service does not return unverified data.

Responses use the shared `ApiResponse` envelope. Invalid dates, enum values, UUIDs, range sizes, and limits return `400`; unauthenticated requests return `401`; unauthorized role/course/status access returns `403`.
