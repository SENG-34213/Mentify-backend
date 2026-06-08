# Mentify Backend

Mentify backend monorepo for Spring Boot microservices. Authentication is centralized through Keycloak, and backend services validate Keycloak-issued JWT access tokens with Spring Security OAuth2 Resource Server support.

## Local Keycloak Setup

### 1. Copy environment file

```bash
cp .env.example .env
```

Update `.env` values before starting services. Do not commit `.env` or real secrets.

### 2. Start Keycloak

```bash
docker compose up -d mentify-keycloak-db mentify-keycloak
```

Keycloak runs at:

```txt
http://localhost:8180
```

The local Docker setup imports `keycloak/mentify-realm.json` on first startup. If the named database volume already exists, remove the local Keycloak volume or configure the realm manually in the admin console.

### 3. Open Keycloak Admin Console

```txt
http://localhost:8180
```

Sign in with:

```txt
Username: KEYCLOAK_ADMIN_USERNAME
Password: KEYCLOAK_ADMIN_PASSWORD
```

Use the `master` realm only for Keycloak administration.

### 4. Verify Realm

Application users, clients, and roles must live in this realm:

```txt
mentify
```

### 5. Verify Realm Roles

The `mentify` realm must contain these realm roles:

```txt
SUPER_ADMIN
ADMIN
TEACHER
STUDENT
PARENT
```

### 6. Verify Clients

Frontend public client:

```txt
Client ID: mentify-web-client
Client authentication: Off
Authorization: Off
Standard flow: On
Direct access grants: On for local testing only
Root URL: http://localhost:5173
Home URL: http://localhost:5173
Valid redirect URIs: http://localhost:5173/*, http://localhost:3000/*
Valid post logout redirect URIs: http://localhost:5173/*, http://localhost:3000/*
Web origins: http://localhost:5173, http://localhost:3000
```

Backend confidential client:

```txt
Client ID: mentify-backend-client
Client authentication: On
Authorization: Off
Service accounts roles: On
Standard flow: Off
Direct access grants: Off
```

The backend client is created for future service-to-Keycloak communication. This repo does not implement Keycloak Admin Client user provisioning yet.

### 7. Create Local Test Users

Create these users manually inside the `mentify` realm and set passwords as non-temporary:

```txt
admin@mentify.com
Role: ADMIN
Password: Admin@123

student@mentify.com
Role: STUDENT
Password: Student@123

teacher@mentify.com
Role: TEACHER
Password: Teacher@123
```

### 8. Configure Spring Boot Services

Each protected servlet service should use the shared security foundation and set:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/mentify}

mentify:
  security:
    enabled: true
    principal-claim: preferred_username
```

`user-service` is configured as the first protected backend service. `api-gateway` validates JWTs at the gateway level and forwards to protected backend services.

### 9. Get a Local Access Token

Use Postman or curl against the frontend public client:

```txt
POST http://localhost:8180/realms/mentify/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded
```

Form body:

```txt
client_id=mentify-web-client
grant_type=password
username=admin@mentify.com
password=Admin@123
```

Use the returned access token for backend calls:

```txt
Authorization: Bearer <access_token>
```

### 10. Verify Test Endpoints

```txt
GET /api/v1/auth/public-test
GET /api/v1/auth/protected-test
GET /api/v1/auth/admin-test
GET /api/v1/auth/student-test
```

Expected results:

```txt
Public endpoint without token: 200
Protected endpoint without token: 401
Protected endpoint with valid token: 200
ADMIN endpoint with ADMIN token: 200
ADMIN endpoint with STUDENT token: 403
STUDENT endpoint with STUDENT token: 200
```

## Security Notes

Do not commit credentials, tokens, client secrets, private keys, production realm exports, or `.env` files. `.env.example` and the local realm import contain placeholders only.
