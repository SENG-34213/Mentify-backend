# Calendar Service Foundation Test Register

Ticket: MENT-#81  
Service: `calender-service`  
Scope: foundation only; event persistence and calendar APIs are follow-up work.

| Test ID | Scenario | Evidence | Expected result |
|---|---|---|---|
| TC-81-01 | Maven module integration | `mvn -pl services/calender-service -am verify` | Module builds without breaking the reactor |
| TC-81-02 | Foundation application bootstrap | `CalendarServiceApplicationTests` | Application class is available |
| TC-81-03 | Shared authentication and role mapping | `SecurityConfig` + common-lib | Existing Keycloak converter/security configuration is reused |
| TC-81-04 | Missing authentication | `CalendarFoundationIntegrationTest` | Protected request returns `401` |
| TC-81-05 | Insufficient role | `CalendarFoundationIntegrationTest` | Student role receives `403` for admin operation |
| TC-81-06 | Database connectivity | H2 test profile; staging PostgreSQL smoke test | Database-backed context starts |
| TC-81-07 | Migration initialization | `V1__calendar_foundation.sql` | Flyway initializes without event tables |
| TC-81-08 | Central configuration | `calender-service.yml` + optional local import | Config Server values override bootstrap defaults |
| TC-81-09 | Service discovery | Eureka configuration and staging smoke test | Service registers as `calender-service` |
| TC-81-10 | Healthy service | `CalendarFoundationIntegrationTest` | `/actuator/health` returns `UP` |
| TC-81-11 | Database readiness | `/actuator/health/readiness` and staging failure test | Readiness includes the database indicator |
| TC-81-12 | Operational information protection | Actuator exposure configuration and test | `/actuator/env` is not exposed; health details are hidden |
| TC-81-13 | Local startup | `docs/PROJECT_SETUP.md` | Documented port, database, and startup commands are available |
| TC-81-14 | Automated quality checks | `.github/workflows/ci-cd.yaml` | Lint, build, unit, and integration matrices include the service |
| TC-81-15 | Existing-service regression | Maven reactor build and CI | Existing modules remain in the reactor |
| TC-81-16 | Staging smoke test | `docs/PROJECT_SETUP.md` | Startup, auth, discovery, and health checks are defined |

## Local verification

Run with a configured Maven cache:

```bash
mvn -B -pl services/calender-service -am verify
```

Latest local verification: the clean focused reactor build completed successfully with all six
`CalendarFoundationIntegrationTest` cases passing. The test uses H2 locally, validates Flyway
initialization, and exercises the shared JWT security chain and database-aware readiness endpoint.
CI integration runs use PostgreSQL through the `CALENDER_TEST_DATASOURCE_*` variables.
