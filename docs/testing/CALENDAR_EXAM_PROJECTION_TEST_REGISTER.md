# Exam-to-Calendar Projection Test Register (MENT-#85)

| ID | Scenario | Expected result |
|---|---|---|
| TC85-01 | Schedule eligible Exam | One mapped `EXAM` Calendar event |
| TC85-02 | Change scheduled Exam title/time | Existing projection refreshed |
| TC85-03 | Repeat unchanged projection | No duplicate or unnecessary save |
| TC85-04 | Concurrent projection | Unique source pair permits one projection |
| TC85-05 | Cancel Exam | Projection retained as `CANCELLED` |
| TC85-06 | Repeat cancellation | No-op and no reactivation |
| TC85-07 | Draft Exam | No projection |
| TC85-08 | Invalid service caller | 401/403 and no mutation |
| TC85-09 | Frontend/internal endpoint misuse | Rejected without service token |
| TC85-10 | Calendar unavailable | Exam operation remains successful; failure recorded |
| TC85-11 | Reconciliation after outage | Current Exam state converges idempotently |
| TC85-12 | Invalid projection payload | Structured 400 and no partial event |
| TC85-13 | Student/Teacher query | Existing course/enrollment visibility applies |
| TC85-14 | Manual API targets Exam event | Source-managed operation rejected |
| TC85-15 | Logs/attempt records | Safe Exam UUID/status evidence; no token or PII |

Automated verification:

```bash
mvn -s maven-verification-settings.xml -pl services/exam-service,services/calender-service -am test
```
