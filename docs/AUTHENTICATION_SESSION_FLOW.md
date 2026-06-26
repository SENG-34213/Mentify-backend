# Authentication Session Flow

## Login

`POST /api/v1/auth/login`

The backend authenticates the submitted email and password through Keycloak's token endpoint using the password grant. Mentify does not generate access tokens or refresh tokens.

Successful responses use the common `ApiResponse` shape:

```json
{
  "statusCode": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<refresh-token>",
    "tokenType": "Bearer",
    "expiresIn": 300,
    "refreshExpiresIn": 1800,
    "scope": "openid email profile",
    "issuedAt": "2026-06-26T00:00:00Z",
    "user": {}
  }
}
```

## Refresh Access Token

`POST /api/v1/auth/refresh`

Request:

```json
{
  "refreshToken": "<refresh-token>"
}
```

The endpoint is anonymous at the Spring Security layer because the access token may already be expired. The refresh token is validated by Keycloak using the `refresh_token` grant.

Successful response:

```json
{
  "statusCode": 200,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "<new-jwt>",
    "refreshToken": "<new-refresh-token-if-rotated>",
    "tokenType": "Bearer",
    "expiresIn": 300,
    "refreshExpiresIn": 1800,
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

Keycloak internal error details are not exposed.

## Logout

`POST /api/v1/auth/logout`

Request:

```json
{
  "refreshToken": "<refresh-token>"
}
```

The backend calls Keycloak's OpenID Connect logout endpoint with the configured auth client credentials and refresh token. This invalidates the Keycloak session or refresh token. Mentify does not store refresh tokens.

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

Mentify validates the email format and looks up the local user profile. If the profile is active, unlocked, and linked to Keycloak, the backend calls Keycloak Admin REST through the Keycloak admin client to execute the `UPDATE_PASSWORD` action email.

Mentify always returns the same response for registered, unregistered, inactive, disabled, suspended, locked, or unlinked users:

```json
{
  "statusCode": 200,
  "message": "If an account matches that email, a password reset link will be sent."
}
```

This protects against email enumeration. Mentify does not generate, store, validate, or log password reset tokens.

## Password Reset Completion

Password reset completion is Keycloak-native in this implementation. The email link takes the user into Keycloak's required-action flow. Keycloak validates the action token, enforces token expiry and one-time use, displays the password update form, and updates the credential.

There is no Mentify `POST /api/v1/auth/reset-password` endpoint in this flow. A custom Mentify reset form would require a separate backend-owned token design or a verified Keycloak token exchange flow.

## Security Notes

Access tokens, refresh tokens, passwords, and client secrets must not be logged.

Authentication errors must use local, controlled messages. Do not return Keycloak `error`, `error_description`, stack traces, token values, passwords, email registration status, or client secrets to API callers.

The API gateway and user-service both allow `/api/v1/auth/refresh`, `/api/v1/auth/logout`, and `/api/v1/auth/forgot-password` without an access token. This is intentional. Keycloak remains the authority for refresh token validation, logout invalidation, password reset tokens, and password update completion.
