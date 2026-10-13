# Notification Service

Notification Service stores the user's in-app inbox and sends email through SMTP. Business
services publish successful, user-facing operations to Kafka; the notification service consumes
those events asynchronously.

## Local setup

Copy `.env.example` to `.env`, then start PostgreSQL, Kafka, and the local SMTP inbox:

```bash
docker compose --profile app-db --profile notifications up -d
```

The compose profiles expose Kafka at `localhost:9092`, SMTP at `localhost:1025`, and Mailpit's
web inbox at `http://localhost:8025`. The app database initialization script creates
`notification_db` along with the other service databases.

Start the service registry and Config Server before the Notification Service. Run the service from
the repository root:

```bash
mvn -pl services/notification-service spring-boot:run
```

It registers with Eureka as `notification-service`, listens on port `8093`, and is reachable
through the API Gateway at `/api/v1/notifications/**`.

## SMTP configuration

The development defaults are for Mailpit. For another SMTP provider, configure these environment
variables in the runtime environment (do not commit credentials):

| Variable | Purpose |
| --- | --- |
| `SMTP_HOST` | SMTP server hostname |
| `SMTP_PORT` | SMTP server port |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | SMTP credentials, if required |
| `SMTP_AUTH` | Enable SMTP authentication when credentials are configured |
| `SMTP_STARTTLS_ENABLE` | Enable STARTTLS when required by the provider |
| `SMTP_FROM` | Sender address |
| `NOTIFICATION_EMAIL_MAX_ATTEMPTS` | Maximum SMTP attempts before marking delivery failed |
| `NOTIFICATION_EMAIL_RETRY_DELAY_MILLIS` | Retry and polling interval in milliseconds |

The Kafka event's recipient address is taken from the authenticated Keycloak token's `email`
claim. Notifications still appear in-app if a token has no email claim; in that case the email
status is `NOT_REQUESTED`. Account invitations and password recovery remain Keycloak-managed
flows and are not duplicated by this service.

## Kafka event contract

Producers use the `mentify.notification-events` topic and the recipient's JWT subject as the
Kafka key. Configure the broker using `KAFKA_BOOTSTRAP_SERVERS`; consumers use the
`notification-service` group by default. The event contract lives in the shared
`common-lib` module.

The shared `@NotifyOnSuccess` annotation publishes only after its controller method returns a
successful response. Events currently cover course and course-content changes, grade-scale
changes, enrollment changes, assignment creation/publication and submission/grading, quiz
creation/publication/attempt submission, exam lifecycle and results, successful Stripe payment
webhooks, attendance session
operations, material upload/maintenance, calendar events, communication-group actions, payment
initiation, and refund operations. Internal reconciliation, reads, and routine AI chat/generation
are intentionally excluded to avoid noisy email.

Kafka delivery is asynchronous so broker outages do not turn a completed business operation into
an API failure; publish failures are logged. The notification database deduplicates repeated Kafka
deliveries by event ID and recipient. SMTP work is recorded with the in-app notification and
retried by the service, so an SMTP outage does not lose the inbox notification.

## API

All endpoints require a valid Keycloak bearer token. The recipient is always derived from the
token subject; clients cannot select another user's inbox.

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/v1/notifications?page=0&size=20&unreadOnly=false` | Get a page of the caller's notifications |
| `GET` | `/api/v1/notifications/unread-count` | Get the caller's unread count |
| `PATCH` | `/api/v1/notifications/{notificationId}/read` | Mark one of the caller's notifications as read |
| `PATCH` | `/api/v1/notifications/read-all` | Mark all of the caller's notifications as read |
| `DELETE` | `/api/v1/notifications/{notificationId}` | Delete one of the caller's notifications |

The page size is limited to 100. Email addresses are never included in API responses. SMTP
delivery status is included as `NOT_REQUESTED`, `PENDING`, `SENT`, or `FAILED`.
