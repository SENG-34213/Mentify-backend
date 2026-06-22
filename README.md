# Mentify Backend

Mentify backend monorepo for Spring Boot microservices.

Authentication is centralized through Keycloak. Backend services validate Keycloak-issued JWT access tokens with Spring Security OAuth2 Resource Server support.

The user service also exposes a controlled login bridge at:

```txt
POST /api/v1/auth/login
```

The backend validates the request, sends credentials to Keycloak, verifies the local Mentify user profile after successful identity authentication, checks role consistency, and returns the common `ApiResponse` success shape. Passwords are never stored or compared by Mentify services.

Required login bridge environment variables:

```txt
KEYCLOAK_SERVER_URL
KEYCLOAK_REALM
KEYCLOAK_AUTH_CLIENT_ID
KEYCLOAK_AUTH_CLIENT_SECRET
KEYCLOAK_INCLUDE_REFRESH_TOKEN_IN_LOGIN_RESPONSE
```

Active local profiles can log in. Complete `INVITED` profiles are activated on the first successful Keycloak login when roles match. `SUSPENDED`, `DISABLED`, locked, missing, incomplete, or role-mismatched profiles receive controlled error responses. Invalid credentials return the generic `401` message until a known local profile reaches 5 failed attempts. On the 5th failed attempt and later attempts for that locked profile, the response is `403` with `Your account is locked`.

## Documentation

Use these setup guides:

```txt
docs/PROJECT_SETUP.md   Full local project setup
docs/KEYCLOAK_SETUP.md  Keycloak realm, clients, roles, and token testing
docs/testing/README.md  Testing standards, coverage commands, and registers
CHANGELOG.md            Unreleased changes
```

## Quick Start

```bash
cp .env.example .env
docker compose up -d
```

Keycloak runs at:

```txt
http://localhost:8180
```

For full setup, read [Project Setup](docs/PROJECT_SETUP.md).
