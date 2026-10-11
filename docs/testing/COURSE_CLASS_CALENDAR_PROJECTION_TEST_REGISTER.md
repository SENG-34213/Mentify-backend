# MENT-#87 Course Class Schedule Calendar Projection Test Register

| Test ID | Scenario | Expected result |
|---|---|---|
| TC-87-01 | Eligible AttendanceSession created | One correctly mapped `COURSE_CLASS` event is created |
| TC-87-02 | Schedule title/time changes | Existing projection is refreshed |
| TC-87-03 | Session cancelled | Projection is retained as `CANCELLED` |
| TC-87-04 | Repeated projection delivery | No duplicate or unintended update |
| TC-87-05 | Concurrent projection requests | Database uniqueness permits at most one event |
| TC-87-06 | Student queries class events | Only enrolled-course events are returned |
| TC-87-07 | Teacher queries class events | Only assigned-course events are returned |
| TC-87-08 | Unauthorized course filter | No unauthorized course event is disclosed |
| TC-87-09 | Unauthorized service caller | Projection request is rejected |
| TC-87-10 | Manual API targets projection | Source-managed event remains unchanged |
| TC-87-11 | Calendar unavailable | Source operation succeeds; failure is observable |
| TC-87-12 | Reconciliation after outage | Calendar converges without duplicates |
| TC-87-13 | Invalid schedule payload | Structured error; no partial event |
| TC-87-14 | Source ownership review | AttendanceSession ownership and mapping are documented |
| TC-87-15 | Logs and errors inspected | No secrets or unnecessary personal data are exposed |
| TC-87-16 | Documentation review | Mapping, lifecycle, authorization, and recovery are documented |

Automated verification:

```text
mvn -s maven-verification-settings.xml -pl services/attendance-service,services/assignment-service,services/calender-service -am test
```
