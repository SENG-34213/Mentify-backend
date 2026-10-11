# MENT-#86 Assignment and Quiz Calendar Projection Test Register

| Test ID | Scenario | Expected result |
|---|---|---|
| TC-86-01 | Published Assignment created | One mapped `ASSIGNMENT` event is projected |
| TC-86-02 | Assignment title/deadline changes | Existing projection is refreshed |
| TC-86-03 | Published Quiz | One mapped `QUIZ` event is projected |
| TC-86-04 | Quiz availability changes | Existing projection is refreshed |
| TC-86-05 | Draft/unpublished source | No calendar projection is created |
| TC-86-06 | Assignment closed/deleted | Projection is retained as `CANCELLED` |
| TC-86-07 | Quiz deleted | Projection is retained as `CANCELLED` |
| TC-86-08 | Repeated delivery | No duplicate or unintended update |
| TC-86-09 | Concurrent same-source delivery | Database uniqueness permits at most one event |
| TC-86-10 | Assignment and Quiz share UUID | Both source-type projections remain valid |
| TC-86-11 | Unauthorized internal caller | Request is rejected without persistence |
| TC-86-12 | Calendar unavailable | Source operation remains successful; failure is recorded |
| TC-86-13 | Reconciliation after outage | Calendar converges to current source state |
| TC-86-14 | Student/Teacher calendar query | Course and enrollment authorization applies |
| TC-86-15 | Manual API targets projection | Source-managed event cannot be changed manually |
| TC-86-16 | Invalid projection payload | Structured validation error; no partial event |
| TC-86-17 | Logs and failure records | No tokens, credentials, URLs, or unnecessary personal data |
| TC-86-18 | Documentation review | Mapping, lifecycle, security, and recovery are documented |

Automated verification:

```text
mvn -s maven-verification-settings.xml -pl services/assignment-service,services/calender-service -am test
```

The temporary settings file is only needed in environments where Maven defaults to an inaccessible local repository; it is not part of the project changes.
