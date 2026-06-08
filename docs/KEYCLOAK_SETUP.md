# Mentify Keycloak Setup

This guide explains the local Keycloak setup for Mentify authentication.

## 1. Start Keycloak

Create a local env file if it does not exist:

```bash
cp .env.example .env
```

Start Keycloak and its database:

```bash
docker compose up -d
```

Keycloak runs at:

```txt
http://localhost:8180
```

Admin login defaults from `.env.example`:

```txt
Username: admin
Password: change-me
```

Use the `master` realm only for Keycloak administration.

## 2. Realm

The local import creates this application realm:

```txt
mentify
```

Application users, clients, and roles must be created inside the `mentify` realm.

## 3. Realm Roles

The `mentify` realm contains these realm roles:

```txt
SUPER_ADMIN
ADMIN
TEACHER
STUDENT
PARENT
```

This project uses realm roles for now, not client roles.

## 4. Frontend Client

Client:

```txt
Client ID: mentify-web-client
Client type: OpenID Connect
Client authentication: Off
Authorization: Off
Standard flow: On
Direct access grants: On for local testing only
```

Local frontend URLs:

```txt
Root URL: http://localhost:5173
Home URL: http://localhost:5173
Valid redirect URIs: http://localhost:5173/*, http://localhost:3000/*
Valid post logout redirect URIs: http://localhost:5173/*, http://localhost:3000/*
Web origins: http://localhost:5173, http://localhost:3000
```

## 5. Backend Client

Client:

```txt
Client ID: mentify-backend-client
Client type: OpenID Connect
Client authentication: On
Authorization: Off
Service accounts roles: On
Standard flow: Off
Direct access grants: Off
```

This client is created for future backend-to-Keycloak communication, such as Keycloak Admin API calls. User provisioning is not implemented yet.

## 6. Spring Boot Configuration

Protected backend services validate Keycloak access tokens with OAuth2 Resource Server support.

Default issuer URI:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/mentify}
```

Mentify security config:

```yaml
mentify:
  security:
    enabled: true
    principal-claim: preferred_username
```

`principal-claim` controls which JWT claim becomes the Spring principal name. The default is `preferred_username`.

## 7. Role Mapping

Keycloak realm roles appear in the JWT like this:

```json
{
  "realm_access": {
    "roles": ["ADMIN", "STUDENT"]
  }
}
```

The backend converts them into Spring Security authorities:

```txt
ADMIN   -> ROLE_ADMIN
STUDENT -> ROLE_STUDENT
TEACHER -> ROLE_TEACHER
```

This enables annotations like:

```java
@PreAuthorize("hasRole('ADMIN')")
```

## 8. Test Users

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

## 9. Get Local Access Token

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

Use the returned access token:

```txt
Authorization: Bearer <access_token>
```

## 10. Verify Backend Endpoints

Test endpoints in `user-service`:

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

## 11. Troubleshooting

If Keycloak starts but the `mentify` realm is missing, the import probably did not run because an old database volume already existed.

Reset local Keycloak data and import again:

```bash
docker compose down -v
docker compose up -d
```

If port `8180` is busy, set another value in `.env`:

```env
KEYCLOAK_PORT=8280
KEYCLOAK_ISSUER_URI=http://localhost:8280/realms/mentify
```

Then restart:

```bash
docker compose up -d
```
