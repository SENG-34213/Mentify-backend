# Communication-to-Calendar Projection (MENT-#89)

## Source review and implementation decision

Communication Service is authoritative for communication data. The current model was reviewed before adding a Calendar integration:

- `Message` represents chat messages and supports `TEXT`, `IMAGE`, `FILE`, and `TEXT_WITH_ATTACHMENT` types. Its `sentAt` value is the message audit/send timestamp, not a scheduled event date.
- `CommunicationGroup` represents a course communication group and contains a course ID, name, description, and active/archived status.
- The current model has no scheduled-meeting record, dated-announcement record, approved publication date, start/end time, location, or communication event lifecycle.

Therefore, MENT-#89 has no eligible source record to project at this point. No Calendar projection endpoint, client, duplicate communication entity, or timestamp-based workaround is introduced. This is intentional and satisfies the ticket rule that ordinary messages, chat timestamps, and undated announcements must not become calendar events.

When Communication Service gains an explicit schedulable source, the integration must use the contract below.

## Future projection contract

| Source field | Calendar field | Rule |
|---|---|---|
| Scheduled communication ID | `sourceId` | Required; stable source identifier |
| Communication source | `sourceType` | Always `COMMUNICATION` |
| Meeting/announcement title | `title` | Required; source-owned |
| Approved start and end, or approved publication date | `startAt`, `endAt` | Required; no `sentAt`/`createdAt` fallback |
| Course ID | `courseId` | Required for course-scoped records; visibility is `COURSE` |
| Location | `location` | Optional, only when source-owned |
| Calendar-safe description | `description` | Optional; do not copy unnecessary personal data |
| Source lifecycle | `status` | Cancellation retains the Calendar tombstone under existing conventions |

Projection requests must be validated before persistence and upserted by `(COMMUNICATION, sourceId)`. The existing Calendar source uniqueness constraint and source-managed event protection are the required duplicate and manual-edit safeguards. Internal projection calls must use the established service authorization mechanism. Student and teacher queries continue to use Calendar’s existing enrollment/course visibility checks.

## Eligibility and exclusions

Only an explicit scheduled meeting or dated announcement is eligible. The following are permanently excluded unless the Communication model later gives them an approved event schedule:

- ordinary chat messages;
- `Message.sentAt`, `createdAt`, or `updatedAt` used as an event date;
- undated announcements;
- communication-group creation or archive timestamps.

Because no eligible source exists today, there is no projection lifecycle, cancellation callback, or reconciliation job to run for MENT-#89. Once the source contract is implemented, Communication writes must remain successful when Calendar is unavailable; projection failures must be recorded and retried/reconciled using the existing service conventions.

## Operational verification

The source review and exclusion behavior are recorded in [the MENT-89 test register](../testing/COMMUNICATION_CALENDAR_PROJECTION_TEST_REGISTER.md). A future implementation must add source-service and Calendar integration tests before enabling the projection path.

