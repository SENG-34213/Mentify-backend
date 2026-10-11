# Calendar Event Management API Test Register

Ticket: MENT-#83  
Service: `calender-service`

| Test ID | Scenario | Evidence | Expected result |
|---|---|---|---|
| TC-83-01 | Administrator creates valid event | `CalendarEventControllerTest`, `CalendarEventServiceImplTest` | 201 envelope and persisted manual event |
| TC-83-02 | Assigned teacher course authorization | `CalendarEventServiceImplTest`, `CalendarCourseAccessServiceTest`, Course Service client | Course assignment is required before creation |
| TC-83-03 | Teacher uses unassigned course | service authorization path | 403 and no persistence |
| TC-83-04 | Student creates event | `CalendarEventServiceImplTest` | 403 and no persistence |
| TC-83-05 | Forged creator field | request DTO excludes creator; service test | JWT identity is used |
| TC-83-06 | Invalid create/update data | `CalendarEventControllerTest` and MENT-82 validation tests | 400 safe validation envelope |
| TC-83-07 | Visible event retrieval | controller/service tests | DTO is returned |
| TC-83-08 | Hidden event retrieval | `CalendarEventServiceImplTest` | 404 without protected details |
| TC-83-09 | Missing event | service/repository contract | 404 |
| TC-83-10 | Authorized update | service workflow | Editable fields change; protected fields remain |
| TC-83-11 | Unauthorized update | service authorization path | Rejected with unchanged persistence |
| TC-83-12 | Projected event update | `CalendarEventServiceImplTest` | 409 and unchanged event |
| TC-83-13 | Authorized cancellation | service workflow | Status becomes CANCELLED; row remains |
| TC-83-14 | Unauthorized cancellation | service authorization path | Rejected; status unchanged |
| TC-83-15 | Projected event cancellation | service workflow | 409 and unchanged event |
| TC-83-16 | Cancelled event update/cancel | `CalendarEventServiceImplTest` | 409 |
| TC-83-17 | Missing authentication | `CalendarFoundationIntegrationTest` against the calendar endpoint | 401 |
| TC-83-18 | Safe structured errors | `CalendarEventControllerTest`, `CalendarExceptionHandler` | Common envelope without secrets or internals |
| TC-83-19 | API documentation | `CALENDAR_EVENT_MANAGEMENT_API.md` | Paths, schemas, permissions, and errors documented |

## Local verification

```bash
mvn -B -pl services/calender-service -am verify
```

The unit suite uses mocked Course/Enrollment clients. Persistence and security regression tests use the existing calendar test profile; CI integration tests can point those clients at service instances through discovery or the configured URL overrides.
