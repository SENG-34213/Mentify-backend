# Authentication Session Flow

## Login

`POST /api/v1/auth/login`

The backend authenticates the submitted email and password through `auth-service`. Mentify issues
its own signed access and refresh JWTs.

Successful responses use the common `ApiResponse` shape:

```json
{
  "statusCode": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<refresh-token>",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "refreshExpiresIn": 86400,
    "scope": "openid email profile",
    "issuedAt": "2026-06-26T00:00:00Z",
    "user": {}
  }
}
```

Newly registered accounts receive an email from user-service with their registered email and
generated initial password. The auth service stores the credential and issues a normal session.
Users can change the password later through the forgot-password
flow. The login response contains the server-resolved `user.role` (`STUDENT`, `TEACHER`, `ADMIN`, or
`SUPER_ADMIN`). The frontend uses that value for role-specific routing, while API authorization
remains enforced by the existing JWT resource-server checks.

## Refresh Access Token

`POST /api/v1/auth/refresh`

Request:

```json
{
  "refreshToken": "<refresh-token>"
}
```

The endpoint is anonymous at the Spring Security layer because the access token may already be expired.
The refresh token is validated by `auth-service`.

Successful response:

```json
{
  "statusCode": 200,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "<new-jwt>",
    "refreshToken": "<new-refresh-token-if-rotated>",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "refreshExpiresIn": 86400,
    "scope": "openid email profile",
    "issuedAt": "2026-06-26T00:00:00Z"
  }
}
```

Expired, malformed, or revoked refresh tokens return a controlled `401 Unauthorized` response:

```json
{
  "statusCode": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired refresh token",
  "path": "/api/v1/auth/refresh"
}
```

Authentication-service internal error details are not exposed.

## Logout

`POST /api/v1/auth/logout`

Request:

```json
{
  "refreshToken": "<refresh-token>"
}
```

The backend invalidates the stored refresh session associated with the refresh token. Mentify does
not expose token internals to clients.

Successful response:

```json
{
  "statusCode": 200,
  "message": "Logout successful"
}
```

Invalid or expired refresh tokens return the same controlled `401 Unauthorized` response as refresh.

## Forgot Password

`POST /api/v1/auth/forgot-password`

Request:

```json
{
  "email": "student@gmail.com"
}
```

Mentify validates the email format and looks up the local user profile. If the profile is active and
unlocked, the backend creates a password-reset challenge and sends it through the configured SMTP service.

Mentify always returns the same response for registered, unregistered, inactive, disabled, suspended, locked, or unlinked users:

```json
{
  "statusCode": 200,
  "message": "If an account matches that email, a password reset link will be sent."
}
```

This protects against email enumeration. Mentify does not generate, store, validate, or log password reset tokens.

## Password Reset Completion

Password reset completion is handled by the auth service through its reset-token and OTP endpoints.

## Security Notes

Access tokens, refresh tokens, passwords, and client secrets must not be logged.

Authentication errors must use local, controlled messages. Do not return provider errors, stack traces,
token values, passwords, email registration status, or client secrets to API callers.

The API gateway and user-service both allow `/api/v1/auth/refresh`, `/api/v1/auth/logout`, and `/api/v1/auth/forgot-password` without an access token. This is intentional. `auth-service` remains the authority for refresh validation, logout invalidation, password-reset tokens, and password updates.
