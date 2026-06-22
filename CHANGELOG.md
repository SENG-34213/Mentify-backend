# Changelog

## [Unreleased]

- Added the user-service Keycloak login bridge at `POST /api/v1/auth/login`.
- Added local profile status and role consistency checks after successful Keycloak authentication.
- Added first-login activation for complete local `INVITED` profiles after successful Keycloak authentication.
- Added safe authentication event logging and controlled authentication error responses.
- Documented login bridge configuration, behavior, troubleshooting, and refresh-token policy boundary.
