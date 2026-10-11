# MENT-#89 Communication Calendar Projection Test Register

## Scope

The current Communication Service model does not contain an explicit schedulable meeting or dated-announcement record. The expected result for all current communication writes is therefore **no Calendar projection**. `Message.sentAt` is deliberately not treated as a calendar date.

| Test ID | Scenario | Expected result | Actual result | Status |
|---|---|---|---|---|
| TC89-01 | Review Communication source model | No schedulable source exists; this is documented | `Message` and `CommunicationGroup` contain no approved schedule fields | PASS |
| TC89-02 | Eligible meeting or dated announcement | Project one `COMMUNICATION` event | Not applicable until an explicit source is added | N/A by design |
| TC89-03 | Ordinary chat message | No Calendar event | No projection path exists | PASS |
| TC89-04 | Undated announcement | No Calendar event | No announcement schedule exists; no timestamp fallback | PASS |
| TC89-05 | Eligible source data change | Update the existing projection | Not applicable until an eligible source exists | N/A by design |
| TC89-06 | Source cancellation/removal | Retain and cancel the projection | Not applicable until an eligible source exists | N/A by design |
| TC89-07 | Repeated/concurrent projection | No duplicate `(COMMUNICATION, sourceId)` event | Covered by existing Calendar source uniqueness convention; no communication projection is enabled | N/A by design |
| TC89-08 | Student course query | Only enrolled course events | No communication events are produced | PASS |
| TC89-09 | Teacher course query | Only assigned course events | No communication events are produced | PASS |
| TC89-10 | Unauthorized internal projection | Reject without mutation | No communication projection endpoint is exposed | PASS |
| TC89-11 | Manual API targets projection | Reject source-managed mutation | No communication projection can be created by manual APIs | PASS |
| TC89-12 | Calendar unavailable during communication write | Source write remains successful; failure observable | No Calendar call is made by current Communication operations | N/A by design |
| TC89-13 | Recovery after outage | Reconcile to source without duplicates | Not applicable until an eligible source and projection attempt record exist | N/A by design |
| TC89-14 | Invalid projection data | Structured error; no partial event | No projection request contract is enabled | N/A by design |
| TC89-15 | Logs and errors | No secrets or unnecessary personal data | No credentials or message contents are added to Calendar integration logs | PASS |
| TC89-16 | Documentation review | Source, eligibility, mapping, lifecycle, auth, idempotency, and recovery documented | See `COMMUNICATION_CALENDAR_PROJECTION_INTEGRATION.md` | PASS |

## Verification notes

- Ordinary communication regression tests remain the authoritative tests for current message and group behavior.
- Calendar source uniqueness, source-managed protection, authorization, and course visibility are already covered by the Calendar tickets that introduced those conventions.
- TC89-02, TC89-05, TC89-06, TC89-07, TC89-12, TC89-13, and TC89-14 become executable integration scenarios when Communication Service introduces an explicit schedulable source. They must not be simulated by using message timestamps.

