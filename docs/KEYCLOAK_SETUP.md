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

This client is used for backend-to-Keycloak communication, including user provisioning through the Admin API and the controlled login bridge token exchange. For local login testing, enable Direct Access Grants on the confidential backend client or configure a dedicated confidential login client through `KEYCLOAK_AUTH_CLIENT_ID` and `KEYCLOAK_AUTH_CLIENT_SECRET`.

## 6. User Service Login Bridge

Endpoint:

```txt
POST /api/v1/auth/login
Content-Type: application/json
```

Request body:

```json
{
  "identifier": "student@mentify.com",
  "password": "your-password"
}
```

The backend responsibilities are:

```txt
1. Validate identifier and password fields.
2. Exchange credentials with Keycloak through the token endpoint.
3. Load the local Mentify user profile by Keycloak user ID or email.
4. Compare Keycloak realm roles with the local Mentify role.
5. Activate complete local `INVITED` profiles after successful Keycloak authentication.
6. Allow access only when the local profile is active and unlocked.
7. Return only approved token metadata and safe user summary fields.
```

Successful response shape:

```json
{
  "statusCode": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "<access-token>",
    "tokenType": "Bearer",
    "expiresIn": 300,
    "user": {
      "email": "student@mentify.com",
      "role": "STUDENT",
      "accountStatus": "ACTIVE"
    }
  }
}
```

Do not paste real tokens into docs, tickets, or logs.

Failed login behavior:

```txt
Invalid credentials or disabled Keycloak account: 401 with a generic message
Invalid request payload: 400 with field-level validation errors
Incomplete invited, suspended, disabled, locked, missing, inactive, or role-mismatched local profile: 403
Keycloak unavailable or misconfigured: 503 with a controlled service message
```

Refresh token handling is controlled by:

```env
KEYCLOAK_INCLUDE_REFRESH_TOKEN_IN_LOGIN_RESPONSE=true
```

Refresh-token rotation, logout, MFA, account lockout dashboards, and long-term session storage are intentionally deferred to follow-up tickets. This flow does not bypass Keycloak brute-force protection; failed credential validation remains inside Keycloak.

Known local profiles also track failed login attempts. Each failed Keycloak credential response increments `users.login_attempts`; on the 5th failed attempt the backend sets `users.account_non_locked=false` and returns `403` with `Your account is locked`. Later attempts for that locked local profile return the same locked message. Unknown identifiers still receive the generic `401` response.

## 7. Spring Boot Configuration

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

Login bridge configuration:

```yaml
keycloak:
  server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8180}
  realm: ${KEYCLOAK_REALM:mentify}
  auth-client-id: ${KEYCLOAK_AUTH_CLIENT_ID:mentify-backend-client}
  auth-client-secret: ${KEYCLOAK_AUTH_CLIENT_SECRET:change-me}
  include-refresh-token-in-login-response: ${KEYCLOAK_INCLUDE_REFRESH_TOKEN_IN_LOGIN_RESPONSE:true}
```

Safe logging rules:

```txt
Logs may include event type, timestamp, local user ID, Keycloak user ID, role, and a normalized login identifier.
Logs must not include passwords, access tokens, refresh tokens, client secrets, or raw Keycloak error payloads.
```

## 8. Role Mapping

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

## 9. Test Users

Create these users manually inside the `mentify` realm and set passwords as non-temporary:

```txt
admin@mentify.com
Role: ADMIN
Password: <set-local-password>

student@mentify.com
Role: STUDENT
Password: <set-local-password>

teacher@mentify.com
Role: TEACHER
Password: <set-local-password>
```

## 10. Get Local Access Token

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
password=<set-local-password>
```

Use the returned access token:

```txt
Authorization: Bearer <access_token>
```

## 11. Verify Backend Endpoints

Test endpoints in `user-service`:

```txt
GET /api/v1/auth/public-test
POST /api/v1/auth/login
GET /api/v1/auth/protected-test
GET /api/v1/auth/admin-test
GET /api/v1/auth/student-test
```

Expected results:

```txt
Public endpoint without token: 200
Login endpoint without token: 200 for valid active users
Protected endpoint without token: 401
Protected endpoint with valid token: 200
ADMIN endpoint with ADMIN token: 200
ADMIN endpoint with STUDENT token: 403
STUDENT endpoint with STUDENT token: 200
```

## 12. Troubleshooting

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

If login returns `503`, verify:

```txt
KEYCLOAK_SERVER_URL points to the Keycloak base URL, not the realm URL.
KEYCLOAK_REALM matches the imported realm.
KEYCLOAK_AUTH_CLIENT_ID exists in the realm.
KEYCLOAK_AUTH_CLIENT_SECRET matches the configured confidential client secret.
Direct Access Grants are enabled for the selected local login client.
```

If login returns `403` after Keycloak accepts credentials, verify the local user-service database has a matching complete profile, `account_non_locked=true`, `is_active=true`, and a local role that matches the Keycloak realm role. Complete `INVITED` profiles become `ACTIVE` during first successful login; incomplete invited profiles remain blocked.
