# Calendar Role-Based Query API Test Register (MENT-#84)

| ID | Scenario | Expected result |
|---|---|---|
| TC84-01 | Unauthenticated `/events` | 401 |
| TC84-02 | Unauthenticated `/upcoming` | 401 |
| TC84-03 | Student query | Only global, student-role, enrolled-course, and owned-private events |
| TC84-04 | Teacher query | Only global, teacher-role, assigned-course, and owned-private events |
| TC84-05 | Admin query | Administrative visibility policy applies |
| TC84-06 | Private event owned by another user | Not returned |
| TC84-07 | Unauthorized `courseId` filter | 403 without event leakage |
| TC84-08 | Missing start/end | 400 |
| TC84-09 | End before or equal to start | 400 |
| TC84-10 | Range over configured maximum | 400 |
| TC84-11 | Event overlaps range | Returned |
| TC84-12 | Event begins exactly at range end | Excluded |
| TC84-13 | Event ends exactly at range start | Excluded |
| TC84-14 | All-day event | Point-at-start convention applied |
| TC84-15 | Type filter | Only requested type |
| TC84-16 | Status filter | Cancelled requires administrator |
| TC84-17 | Deterministic ordering | Start time, then event ID |
| TC84-18 | Upcoming feed | Future events only |
| TC84-19 | Invalid or excessive limit | 400 |
| TC84-20 | Course/enrollment dependency failure | 503, safe message |
| TC84-21 | Malformed UUID/enum | 400, structured safe error |

Automated coverage includes the calendar service unit, repository, controller, foundation security, and integration suites. Run with:

```bash
mvn -pl services/calender-service -am test
```
