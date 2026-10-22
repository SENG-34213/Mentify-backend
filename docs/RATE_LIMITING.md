# API rate limiting

Rate limiting is enforced at the API Gateway, before a request reaches a backend
service. The gateway uses Redis as the shared token-bucket store so limits remain
consistent when multiple gateway instances are running.

## Policies

| Traffic | Identity | Default policy |
| --- | --- | --- |
| Login | Client IP | 0.5 requests/second, burst 5 |
| Token refresh | Client IP | 1 request/second, burst 10 |
| Password recovery | Client IP | 0.2 requests/second, burst 3 |
| AI chat | Authenticated subject, otherwise IP | 1 request/second, burst 4 |
| Uploads and attachment writes | Authenticated subject, otherwise IP | 2 requests/second, burst 6 |
| WebSocket handshakes | Authenticated subject, otherwise IP | 1 request/second, burst 5 |
| Other `/api/**` traffic | Authenticated subject, otherwise IP | 20 requests/second, burst 40 |

The limits are deliberately applied at the gateway because the frontend uses one
public API origin while the backend is split across services. Internal service-to-
service traffic does not pass through the gateway and is not affected by these
limits.

## Configuration

Set `GATEWAY_RATE_LIMIT_ENABLED=false` only for local troubleshooting. The limit
values can be overridden with the `RATE_LIMIT_*` environment variables in
`.env`; the defaults are suitable starting points, not capacity guarantees.

`GATEWAY_RATE_LIMIT_TRUST_FORWARDED_HEADERS` is false by default. Enable it only
when the gateway is behind a trusted proxy that overwrites `X-Forwarded-For`.

When Redis is unavailable, the gateway fails open and continues serving requests;
this prevents Redis from becoming a single point of application failure. Redis
availability must still be monitored because rate-limit enforcement is degraded
until it returns.

Clients receive HTTP `429 Too Many Requests` with `Retry-After`,
`X-RateLimit-Limit`, and `X-RateLimit-Remaining` headers.
