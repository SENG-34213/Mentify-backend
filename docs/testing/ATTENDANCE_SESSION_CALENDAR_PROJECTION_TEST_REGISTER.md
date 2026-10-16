# MENT-#88 Scheduled Attendance Session Calendar Projection Test Register

| Test ID | Scenario | Expected result |
|---|---|---|
| TC-88-01 | AttendanceSession source reviewed | `AttendanceSession` is documented as the authoritative planned-session source |
| TC-88-02 | Future OPEN session created | One correctly mapped `ATTENDANCE` event is created |
| TC-88-03 | Session title/time changes | Existing projection is refreshed |
| TC-88-04 | Session cancelled/completed/removed | Projection is retained as `CANCELLED` |
| TC-88-05 | Repeated/concurrent delivery | No duplicate event is created |
| TC-88-06 | AttendanceRecord created or marked | No event is created for the record/outcome |
| TC-88-07 | Student queries sessions | Only enrolled-course events are returned |
| TC-88-08 | Teacher queries sessions | Only assigned-course events are returned |
| TC-88-09 | Unauthorized course filter | No unauthorized event is disclosed |
| TC-88-10 | Unauthorized service caller | Projection request is rejected |
| TC-88-11 | Manual API targets session event | Source-managed event remains unchanged |
| TC-88-12 | Calendar unavailable | Attendance operation succeeds; failure is observable |
| TC-88-13 | Reconciliation after outage | Calendar converges without duplicates |
| TC-88-14 | Invalid session projection | Structured error; no partial event |
| TC-88-15 | Logs/errors inspected | No secrets or unnecessary personal data are exposed |
| TC-88-16 | Documentation reviewed | Source, eligibility, mapping, lifecycle, and recovery are documented |

Automated verification:

```text
mvn -s maven-verification-settings.xml -pl services/attendance-service,services/assignment-service,services/calender-service -am test
```
