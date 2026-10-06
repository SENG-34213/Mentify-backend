# Changelog

## [Unreleased]

- Added the physical Exam persistence model (#39): UUID-backed Exam entity, type/status enums, Flyway schema with integrity constraints and course/date indexes, repository query methods, and persistence tests.
- Added exam-service foundation (#38): Keycloak JWT security via common-lib, JWT-derived user identity/roles, safe global exception handling, health checks, env-based config in config-server (exam-service.yml, port 8089), gateway route /api/v1/exams/**, CI/docker/env wiring. Schema is managed by Flyway (db/migration, same setup as user-service) with Hibernate ddl-auto: validate.
- Added the user-service Keycloak login bridge at `POST /api/v1/auth/login`.
- Added local profile status and role consistency checks after successful Keycloak authentication.
- Added first-login activation for complete local `INVITED` profiles after successful Keycloak authentication.
- Added safe authentication event logging and controlled authentication error responses.
- Documented login bridge configuration, behavior, troubleshooting, and refresh-token policy boundary.
- Added Keycloak-native forgot password request flow at `POST /api/v1/auth/forgot-password`.
- Added email-enumeration-safe forgot password responses and safe reset event logging.
