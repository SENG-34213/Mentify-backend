# Mentify Backend

Mentify backend monorepo for Spring Boot microservices.

Authentication is centralized through Keycloak. Backend services validate Keycloak-issued JWT access tokens with Spring Security OAuth2 Resource Server support.

## Documentation

Use these setup guides:

```txt
docs/PROJECT_SETUP.md   Full local project setup
docs/KEYCLOAK_SETUP.md  Keycloak realm, clients, roles, and token testing
docs/testing/README.md  Testing standards, coverage commands, and registers
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
