# Calendar Event Domain and Persistence Test Register

Ticket: MENT-#82  
Service: `calender-service`

| Test ID | Scenario | Evidence | Expected result |
|---|---|---|---|
| TC-82-01 | Valid event validation | `CalendarEventValidationTest` | Valid timed event is accepted |
| TC-82-02 | Title and location boundaries | `CalendarEventValidationTest` | 200/255 characters pass; larger values fail |
| TC-82-03 | Timed/all-day rules | `CalendarEventValidationTest` | Timed events require end; all-day events may omit it |
| TC-82-04 | Chronology | `CalendarEventValidationTest` | End before start fails; equal timestamps are valid |
| TC-82-05 | Conditional references | `CalendarEventValidationTest` | Course, role, and external source requirements are enforced |
| TC-82-06 | Full persistence mapping | `CalendarEventRepositoryIntegrationTest` | All fields and generated audit timestamps round-trip |
| TC-82-07 | Manual source null behavior | `CalendarEventRepositoryIntegrationTest` | Multiple manual events with null source IDs are accepted |
| TC-82-08 | Duplicate source protection | `CalendarEventRepositoryIntegrationTest` | Duplicate `(sourceType, sourceId)` is rejected |
| TC-82-09 | Cross-source type identity | `CalendarEventRepositoryIntegrationTest` | Same source ID with different source types is accepted |
| TC-82-10 | Audit and cancellation preservation | `CalendarEventRepositoryIntegrationTest` | Creator/creation time/source references remain stable; status changes to CANCELLED |
| TC-82-11 | Overlap query | `CalendarEventRepositoryIntegrationTest` | Inclusive boundaries and events beginning before the range are returned |
| TC-82-12 | Query filters and ordering | `CalendarEventRepositoryIntegrationTest` | Optional filters apply and chronology is deterministic |
| TC-82-13 | Database enum constraints | `CalendarEventRepositoryIntegrationTest` | Unsupported persisted enum values are rejected |
| TC-82-14 | Repeat-safe migration | `CalendarEventRepositoryIntegrationTest` | Flyway records V2 once and repeated migrate is a no-op |
| TC-82-15 | Concurrent duplicate creation | `CalendarEventRepositoryIntegrationTest` | Exactly one concurrent source pair succeeds |

## Local verification

```bash
mvn -B -pl services/calender-service -am verify
```

The test profile uses H2 locally and supports PostgreSQL through `CALENDER_TEST_DATASOURCE_*` variables in CI integration jobs. Coverage and the full reactor regression remain enforced by the repository Maven/CI configuration.
