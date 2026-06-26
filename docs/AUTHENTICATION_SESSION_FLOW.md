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

## Security Notes

Access tokens, refresh tokens, passwords, and client secrets must not be logged.

Authentication errors must use local, controlled messages. Do not return Keycloak `error`, `error_description`, stack traces, token values, passwords, or client secrets to API callers.

The API gateway and user-service both allow `/api/v1/auth/refresh` and `/api/v1/auth/logout` without an access token. This is intentional. Keycloak remains the authority for refresh token validation and logout invalidation.
