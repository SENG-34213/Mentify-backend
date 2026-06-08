# Test Register: Keycloak Authentication Foundation

## Summary

Feature scope:

```txt
Keycloak JWT validation
Realm role conversion
Public/protected endpoint authorization
ADMIN/STUDENT method-level role checks
Gateway token enforcement
```

Related implementation files:

```txt
common-lib/src/main/java/com/mentify/common/security/KeycloakRoleConverter.java
common-lib/src/main/java/com/mentify/common/security/KeycloakJwtAuthenticationConverter.java
common-lib/src/main/java/com/mentify/common/security/SecurityConfig.java
services/user-service/src/main/java/com/mentify/controller/AuthTestController.java
cloud/api-gateway/src/main/java/com/mentify/gateway/config/GatewaySecurityConfig.java
```

Related test files:

```txt
common-lib/src/test/java/com/mentify/common/security/KeycloakRoleConverterTest.java
common-lib/src/test/java/com/mentify/common/security/KeycloakJwtAuthenticationConverterTest.java
common-lib/src/test/java/com/mentify/common/security/SecurityConfigTest.java
services/user-service/src/test/java/com/mentify/controller/AuthTestControllerTest.java
cloud/api-gateway/src/test/java/com/mentify/gateway/config/GatewaySecurityConfigTest.java
```

## Test Cases

| Test Case ID | Test Case Name | Related Requirement | Related Test File | Type | Priority | Preconditions | Input | Expected Output | Actual Output | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-AUTH-01 | Convert ADMIN realm role to Spring authority | MENT-AUTH-ROLE-MAPPING | `KeycloakRoleConverterTest` | Unit | P1 Must Pass | JWT contains `realm_access.roles=[ADMIN]` | `converter.convert(jwt)` | Authority contains `ROLE_ADMIN` | Passes in Maven verify | Pass |
| TC-AUTH-02 | Convert STUDENT realm role to Spring authority | MENT-AUTH-ROLE-MAPPING | `KeycloakRoleConverterTest` | Unit | P1 Must Pass | JWT contains `realm_access.roles=[STUDENT]` | `converter.convert(jwt)` | Authority contains `ROLE_STUDENT` | Passes in Maven verify | Pass |
| TC-AUTH-03 | Convert multiple realm roles to Spring authorities | MENT-AUTH-ROLE-MAPPING | `KeycloakRoleConverterTest` | Unit | P1 Must Pass | JWT contains `realm_access.roles=[ADMIN, STUDENT]` | `converter.convert(jwt)` | Authorities contain `ROLE_ADMIN` and `ROLE_STUDENT` | Passes in Maven verify | Pass |
| TC-AUTH-04 | Return empty authorities when `realm_access` is missing | MENT-AUTH-ROLE-MAPPING | `KeycloakRoleConverterTest` | Unit | P1 Must Pass | JWT has no `realm_access` claim | `converter.convert(jwt)` | Empty authority collection | Passes in Maven verify | Pass |
| TC-AUTH-05 | Return empty authorities when `roles` is missing | MENT-AUTH-ROLE-MAPPING | `KeycloakRoleConverterTest` | Unit | P1 Must Pass | JWT has `realm_access` without `roles` | `converter.convert(jwt)` | Empty authority collection | Passes in Maven verify | Pass |
| TC-AUTH-06 | Use preferred username as principal | MENT-AUTH-PRINCIPAL | `KeycloakJwtAuthenticationConverterTest` | Unit | P1 Must Pass | JWT contains `preferred_username` and ADMIN role | `converter.convert(jwt)` | Principal is preferred username and authority is `ROLE_ADMIN` | Passes in Maven verify | Pass |
| TC-AUTH-07 | Fall back to subject for blank principal claim | MENT-AUTH-PRINCIPAL | `KeycloakJwtAuthenticationConverterTest` | Unit | P1 Must Pass | JWT has blank `preferred_username` | `converter.convert(jwt)` | Principal is JWT subject | Passes in Maven verify | Pass |
| TC-AUTH-08 | Use custom configured principal claim | MENT-AUTH-PRINCIPAL | `KeycloakJwtAuthenticationConverterTest` | Unit | P1 Must Pass | Principal claim configured to `email` | `converter.convert(jwt)` | Principal is email claim | Passes in Maven verify | Pass |
| TC-AUTH-09 | Servlet security allows configured public endpoint | MENT-AUTH-SERVICE-SECURITY | `SecurityConfigTest` | Integration | P1 Must Pass | Security filter chain enabled | `GET /api/v1/auth/public-test` without token | HTTP 200 | Passes in Maven verify | Pass |
| TC-AUTH-10 | Servlet security rejects protected endpoint without token | MENT-AUTH-SERVICE-SECURITY | `SecurityConfigTest` | Integration | P1 Must Pass | Security filter chain enabled | `GET /protected-resource` without token | HTTP 401 | Passes in Maven verify | Pass |
| TC-AUTH-11 | Public endpoint allows anonymous access | MENT-AUTH-ENDPOINTS | `AuthTestControllerTest` | Integration | P1 Must Pass | Security filter chain enabled | `GET /api/v1/auth/public-test` without token | HTTP 200 with success body | Passes in Maven verify | Pass |
| TC-AUTH-12 | Protected endpoint rejects missing token | MENT-AUTH-ENDPOINTS | `AuthTestControllerTest` | Integration | P1 Must Pass | Security filter chain enabled | `GET /api/v1/auth/protected-test` without token | HTTP 401 | Passes in Maven verify | Pass |
| TC-AUTH-13 | Protected endpoint accepts valid JWT | MENT-AUTH-ENDPOINTS | `AuthTestControllerTest` | Integration | P1 Must Pass | Mock JWT with subject, email, username | `GET /api/v1/auth/protected-test` with JWT | HTTP 200 with user claims | Passes in Maven verify | Pass |
| TC-AUTH-14 | ADMIN endpoint accepts ADMIN role | MENT-AUTH-RBAC | `AuthTestControllerTest` | Integration | P1 Must Pass | Mock JWT authority `ROLE_ADMIN` | `GET /api/v1/auth/admin-test` | HTTP 200 | Passes in Maven verify | Pass |
| TC-AUTH-15 | ADMIN endpoint rejects STUDENT role | MENT-AUTH-RBAC | `AuthTestControllerTest` | Integration | P1 Must Pass | Mock JWT authority `ROLE_STUDENT` | `GET /api/v1/auth/admin-test` | HTTP 403 | Passes in Maven verify | Pass |
| TC-AUTH-16 | STUDENT endpoint accepts STUDENT role | MENT-AUTH-RBAC | `AuthTestControllerTest` | Integration | P1 Must Pass | Mock JWT authority `ROLE_STUDENT` | `GET /api/v1/auth/student-test` | HTTP 200 | Passes in Maven verify | Pass |
| TC-AUTH-17 | Gateway allows configured public endpoint | MENT-AUTH-GATEWAY | `GatewaySecurityConfigTest` | Integration | P1 Must Pass | Gateway security filter chain enabled | `GET /api/v1/auth/public-test` without token | HTTP 200 | Passes in Maven verify | Pass |
| TC-AUTH-18 | Gateway rejects protected endpoint without token | MENT-AUTH-GATEWAY | `GatewaySecurityConfigTest` | Integration | P1 Must Pass | Gateway security filter chain enabled | `GET /protected-gateway-resource` without token | HTTP 401 | Passes in Maven verify | Pass |

## Coverage Notes

JaCoCo is scoped to the Keycloak authentication classes introduced in this ticket:

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

Generate reports and run checks:

```bash
sh cloud/config-server/mvnw -f pom.xml -pl common-lib verify
sh cloud/config-server/mvnw -f pom.xml -pl services/user-service -am verify
sh cloud/config-server/mvnw -f pom.xml -pl cloud/api-gateway -am verify
```

Report locations:

```txt
common-lib/target/site/jacoco/index.html
services/user-service/target/site/jacoco/index.html
cloud/api-gateway/target/site/jacoco/index.html
```

Latest local verification:

```txt
common-lib: 100.00% lines, 80.00% branches
user-service: 100.00% lines, 100.00% branches
api-gateway: 100.00% lines, 100.00% branches
```

Existing unrelated classes are excluded from this ticket's coverage gate. They should be tested by the tickets that modify or extend them.

## DB Verification Note

The current auth test endpoints validate security behavior only and do not create, update, or delete database records. Direct DB-state assertions are therefore not applicable to this ticket. Future user registration, course creation, and provisioning tickets must include service-to-database integration tests with seed and cleanup steps.
