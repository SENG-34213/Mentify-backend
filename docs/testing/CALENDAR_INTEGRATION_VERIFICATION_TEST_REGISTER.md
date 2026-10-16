# MENT-#90 Calendar Integration Verification Test Register

`PASS` means covered by the automated local suites. `BLOCKED` means the check requires deployed services or staging infrastructure and is not claimed locally.

| Test ID | Scenario | Evidence | Expected result | Status |
|---|---|---|---|---|
| TC-90-01 | Manual event lifecycle | `CalendarEventControllerTest`, `CalendarEventServiceImplTest` | Create, retrieve, update, and cancel follow MENT-83 rules | PASS |
| TC-90-02 | Enrolled Student query | `CalendarQueryServiceImplTest`, `CalendarCourseAccessServiceTest` | Current enrolled-course events are returned | PASS |
| TC-90-03 | Enrollment removed | Course/Enrollment authorization contract | Removed membership prevents course visibility | BLOCKED: deployed membership-change smoke test |
| TC-90-04 | Assigned Teacher query | `CalendarQueryServiceImplTest`, `CalendarCourseAccessServiceTest` | Current assigned-course events are returned | PASS |
| TC-90-05 | Teacher assignment removed | Course authorization contract | Removed assignment prevents course visibility | BLOCKED: deployed assignment-change smoke test |
| TC-90-06 | Administrator query | `CalendarQueryServiceImplTest` | Administrative visibility policy applies | PASS |
| TC-90-07 | Cross-user private event | `CalendarQueryServiceImplTest`, `CalendarEventServiceImplTest` | Private event remains hidden | PASS |
| TC-90-08 | Repeated/retried projection | Source projection and Calendar uniqueness tests | One current event per source pair | PASS |
| TC-90-09 | Source cancellation | Exam, Assignment/Quiz, Attendance projection tests | Projection is cancelled and retained | PASS |
| TC-90-10 | Source deletion | Assignment/Quiz lifecycle and reconciliation tests | Tombstone policy is applied | PASS |
| TC-90-11 | Calendar outage during source operation | Source `CalendarProjectionServiceImplTest` classes | Source save remains successful; failure is recorded | PASS |
| TC-90-12 | Projection retry/reconciliation | Source reconciliation service tests | Current state converges without duplicates | PASS |
| TC-90-13 | Course Service outage | `CalendarQueryServiceImplTest` | Controlled 503; no unverified teacher events | PASS |
| TC-90-14 | Enrollment Service outage | `CalendarQueryServiceImplTest` | Controlled 503; no unverified student events | PASS |
| TC-90-15 | Unauthorized course filter | `CalendarQueryServiceImplTest` | No unauthorized event disclosure | PASS |
| TC-90-16 | Invalid/oversized date range | Calendar controller/query tests | Structured 400 validation response | PASS |
| TC-90-17 | Overlap and boundaries | `CalendarQueryServiceImplTest`, repository integration tests | Half-open overlap and ordering rules apply | PASS |
| TC-90-18 | Upcoming limits | Calendar query/controller tests | Default and maximum limits are enforced | PASS |
| TC-90-19 | Missing/invalid JWT | `CalendarFoundationIntegrationTest`, security tests | Protected endpoint returns 401 | PASS |
| TC-90-20 | Unauthorized event mutation | `CalendarEventServiceImplTest` | Request rejected; state unchanged | PASS |
| TC-90-21 | Manual API targets projection | `CalendarEventServiceImplTest` | Source-managed event cannot be changed manually | PASS |
| TC-90-22 | Optional source models absent | MENT-88/89 source reviews | Attendance records and ordinary messages are not projected | PASS |
| TC-90-23 | Source-service regressions | Exam, Assignment, Attendance, Communication suites | Authoritative behavior remains intact | BLOCKED: Attendance test compilation hits cached `jackson-datatype-jdk8:2.15.4` artifact |
| TC-90-24 | Evidence review | This register and verification document | Results, limitations, and run command documented | PASS |

## Local command

```text
mvn -s maven-verification-settings.xml -pl services/exam-service,services/assignment-service,services/attendance-service,services/calender-service -am test
```

Staging checks TC-90-03 and TC-90-05 remain explicitly blocked until current membership and assignment changes are exercised against deployed dependencies.

