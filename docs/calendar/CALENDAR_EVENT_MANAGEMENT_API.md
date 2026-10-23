# Calendar Event Management API

Ticket: MENT-#83  
Service: `calender-service`  
Base path: `/api/v1/calendar/events`

All endpoints require a Keycloak bearer token. The gateway route is `/api/v1/calendar/**` and forwards to `calender-service`.

## Endpoints

| Method | Path | Roles | Purpose |
|---|---|---|---|
| POST | `/api/v1/calendar/events` | `ADMIN`, `SUPER_ADMIN`, `TEACHER` | Create a manual event |
| GET | `/api/v1/calendar/events/{eventId}` | `STUDENT`, `TEACHER`, `ADMIN`, `SUPER_ADMIN` | Retrieve one visible event |
| PUT | `/api/v1/calendar/events/{eventId}` | `ADMIN`, `SUPER_ADMIN`, `TEACHER` | Update a non-cancelled manual event |
| POST | `/api/v1/calendar/events/{eventId}/cancel` | `ADMIN`, `SUPER_ADMIN`, `TEACHER` | Cancel a non-cancelled manual event |

## Request body

Create and update use the same DTO. The client may send `title`, `description`, `eventType`, `startDateTime`, `endDateTime`, `allDay`, `courseId`, `visibility`, `targetRole`, and `location`. `createdBy`, `sourceType`, `sourceId`, `status`, `createdAt`, and `updatedAt` are never accepted from the client.

```json
{
  "title": "Term review meeting",
  "description": "Review the upcoming assessment schedule",
  "eventType": "MEETING",
  "startDateTime": "2026-10-12T09:00:00",
  "endDateTime": "2026-10-12T10:00:00",
  "allDay": false,
  "courseId": "00000000-0000-0000-0000-000000000001",
  "visibility": "COURSE",
  "location": "Room 1"
}
```

Timed events require an end timestamp; all-day events may omit it. End must not precede start. `COURSE` visibility requires `courseId`; `ROLE` visibility requires `targetRole`. The MENT-82 domain limits for title, description, location, and supported enum values also apply.

Manual APIs always persist `sourceType=CUSTOM` with no `sourceId`. Existing source-projected events are read-only through these endpoints.

## Authorization behavior

- `SUPER_ADMIN` and `ADMIN` may create and manage global, role, course, and private manual events.
- `TEACHER` may create course events only after Course Service confirms assignment. Teachers may manage their own authorized course events or other authorized course events.
- `STUDENT` cannot create, update, or cancel. Students can retrieve global events, matching role events, private events they own, and course events only when Enrollment Service confirms membership.
- Private events are visible only to their creator and administrators.
- Unauthorized event reads use the not-found response to avoid leaking event existence.
- Creation identity is taken from the JWT `local_user_id` when present, otherwise the JWT subject; client identity fields are not trusted. Course assignment checks recognize either UUID claim so legacy course records remain manageable.

Update preserves the event ID, creator, creation timestamp, and source linkage. Cancellation changes status to `CANCELLED` and retains the row. Cancelled events cannot be updated or cancelled again. Invalid updates are validated before mutation.

## Response envelope

Successful responses use the common `ApiResponse` envelope. The `data` object is `CalendarEventResponse`, not the JPA entity, and includes event fields plus `canEdit` and `canCancel` convenience flags.

```json
{
  "statusCode": 201,
  "message": "Calendar event created successfully",
  "data": {
    "id": "00000000-0000-0000-0000-000000000002",
    "title": "Term review meeting",
    "eventType": "MEETING",
    "startDateTime": "2026-10-12T09:00:00",
    "endDateTime": "2026-10-12T10:00:00",
    "allDay": false,
    "courseId": "00000000-0000-0000-0000-000000000001",
    "sourceType": "CUSTOM",
    "createdBy": "00000000-0000-0000-0000-000000000003",
    "visibility": "COURSE",
    "status": "SCHEDULED",
    "canEdit": true,
    "canCancel": true
  }
}
```

## Error outcomes

`400` means malformed or invalid request data; `401` means missing/invalid authentication; `403` means the role or course authorization failed; `404` means the event/course does not exist or the event is hidden; `409` means a projected/cancelled event cannot be changed or a persistence constraint conflicts; `503` means a required Course or Enrollment Service dependency is unavailable. Error bodies use the common `ApiResponse` envelope and never expose stack traces, credentials, tokens, SQL, or downstream URLs.
