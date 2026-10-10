# Testing Documentation

This folder tracks test strategy, test registers, and coverage notes for Mentify backend tickets.

## Standards

Every feature should include:

```txt
Unit tests for isolated logic
Integration tests for HTTP/service behavior
Coverage report generation
Test register entry
```

Use Given-When-Then test names and Arrange-Act-Assert structure.

## Keycloak Auth Coverage Scope

For the Keycloak authentication foundation ticket, JaCoCo is scoped to the new auth classes only:

```txt
com.mentify.common.security.*
com.mentify.controller.AuthTestController
com.mentify.gateway.config.GatewaySecurityConfig
```

The Maven `verify` phase enforces:

```txt
Line coverage:   >= 80%
Branch coverage: >= 80%
```

Existing unrelated classes are excluded from this ticket's coverage gate. They should be covered by the tickets that modify or extend them.

## Coverage Commands

Generate reports and run coverage checks:

```bash
sh cloud/config-server/mvnw -f pom.xml -pl common-lib verify
sh cloud/config-server/mvnw -f pom.xml -pl services/user-service -am verify
sh cloud/config-server/mvnw -f pom.xml -pl cloud/api-gateway -am verify
```

Run the whole Keycloak auth coverage set with one command:

```bash
sh cloud/config-server/mvnw -f pom.xml -pl services/user-service,cloud/api-gateway -am verify
```

Use `verify`, not only `test`:

```txt
test    -> runs tests only
verify  -> runs tests, generates JaCoCo reports, and checks coverage thresholds
```

Generated reports:

```txt
common-lib/target/site/jacoco/index.html
services/user-service/target/site/jacoco/index.html
cloud/api-gateway/target/site/jacoco/index.html
```

Open the reports manually in a browser:

```bash
open common-lib/target/site/jacoco/index.html
open services/user-service/target/site/jacoco/index.html
open cloud/api-gateway/target/site/jacoco/index.html
```

If the coverage threshold fails, Maven exits with build failure and shows which module missed the required line or branch coverage.

## Current Keycloak Auth Coverage

Latest local verification:

```txt
common-lib: 100.00% lines, 80.00% branches
user-service: 100.00% lines, 100.00% branches
api-gateway: 100.00% lines, 100.00% branches
```

## Testing Documents

Current documents:

```txt
docs/testing/TEST_REGISTER_KEYCLOAK_AUTH.md
docs/testing/ADMIN_USER_REGISTRATION_FLOW.md
docs/testing/ASSIGNMENT_POSTMAN_TESTS.md
docs/testing/QUIZ_POSTMAN_TESTS.md
docs/testing/COMMUNICATION_POSTMAN_TESTS.md
docs/testing/ATTENDANCE_POSTMAN_TESTS.md
```

Before formal submission, export the required PDF deliverables if your course process requires them:

```txt
documents/testing/test-register.pdf
documents/testing/coverage-sprint8.pdf
```
