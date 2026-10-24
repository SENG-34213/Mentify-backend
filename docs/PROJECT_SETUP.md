# Mentify Backend Project Setup

This guide explains how to run the Mentify backend locally.

## 1. Prerequisites

Install these tools before starting:

```txt
Java 17 or newer
Docker Desktop
Docker Compose
Maven or the included Maven wrapper
Git
```

The project currently uses Spring Boot, Spring Cloud, PostgreSQL, JWT authentication, and Maven multi-module structure.

## 2. Clone Repository

```bash
git clone <repository-url>
cd Mentify-backend
```

## 3. Create Local Environment File

```bash
cp .env.example .env
```

Update `.env` if your local ports or credentials need to be different.

Do not commit `.env`.

## 4. Start Required Local Infrastructure

Start the local infrastructure required by the selected services:

```bash
docker compose up -d
```

Expected containers:

```txt
postgres_container_mentify
mentify-redis
mentify-kafka
```

Default ports:

```txt
JWT authentication: auth-service at http://localhost:8085
App PostgreSQL:     localhost:5432
```

## 5. Optional App Database Tooling

The compose file also defines app PostgreSQL and pgAdmin, but they are behind the `app-db` profile.

Start them only when needed:

```bash
docker compose --profile app-db up -d
```

On first startup, the app PostgreSQL container creates the service databases, including:

```txt
mentify
user_db
course_db
calender_db
notification_db
```

Default optional ports:

```txt
App PostgreSQL: localhost:5432
pgAdmin:        http://localhost:5050
```

### pgAdmin Setup

Open pgAdmin:

```txt
http://localhost:5050
```

Login with values from `.env`:

```txt
Email:    PGADMIN_DEFAULT_EMAIL
Password: PGADMIN_DEFAULT_PASSWORD
```

Create a server connection in pgAdmin:

General tab:

```txt
Name: mentify-db
```

Connection tab:

```txt
Host name/address: postgres
Port:              5432
Maintenance DB:    mentify
Username:          postgres
Password:          change-me
Save password:     On
```

Use `postgres` as the host because pgAdmin runs inside Docker and connects to the Postgres service over the Compose network. Do not use container IPs like `172.19.0.2`; Docker can change them after containers are recreated.

If connecting from a desktop database client outside Docker, use:

```txt
Host: localhost
Port: 5432
Database: mentify
Username: postgres
Password: change-me
```

Stop optional app database tooling:

```bash
docker compose stop pgadmin postgres
docker compose rm -f pgadmin postgres
```

If the `postgres` Docker volume already exists, init scripts will not run again. To recreate the local app databases from scratch:

```bash
docker compose --profile app-db down -v
docker compose --profile app-db up -d
```

Use this carefully because it deletes local Docker volumes for this compose project.

## 6. Start Backend Services

Recommended local startup order:

```txt
1. service-registry
2. config-server
3. api-gateway
4. user-service
5. course-service
6. calender-service
7. notification-service
```

If using IntelliJ IDEA, run each Spring Boot application from its main class:

```txt
cloud/service-registry/src/main/java/com/mentify/ServiceRegistryApplication.java
cloud/config-server/src/main/java/com/mentify/configserver/ConfigServerApplication.java
cloud/api-gateway/src/main/java/com/mentify/gateway/ApiGatewayApplication.java
services/user-service/src/main/java/com/mentify/UserServiceApplication.java
services/course-service/src/main/java/com/mentify/CourseServiceApplication.java
services/calender-service/src/main/java/com/mentify/calendar/CalendarServiceApplication.java
services/notification-service/src/main/java/com/mentify/notification/NotificationServiceApplication.java
```

## 7. Authentication Setup

`auth-service` issues Mentify JWT access and refresh tokens. Protected services validate
the token signature using the configured public key and authorize users using the token role.

`user-service` is configured as the first protected backend service.

Protected services use this issuer URI by default:

```txt
http://localhost:8085
```

Calendar Service uses the same JWT resource-server configuration. It does not provide
separate login or password-management endpoints. The default local configuration is loaded from
Config Server using the `calender-service` application name; `application.yml` retains only safe
bootstrap defaults and the optional Config Server import.

Calendar Service runs on port `8092` and uses `calender_db`. Start the app database profile before
starting it:

```bash
docker compose --profile app-db up -d
sh cloud/config-server/mvnw -f pom.xml -pl services/calender-service spring-boot:run
```

Notification Service uses `notification_db`, Kafka, and SMTP. Start the `app-db` and
`notifications` compose profiles to provide PostgreSQL, the Kafka broker, and Mailpit for local
testing. Then follow [Notification Service setup](./NOTIFICATION_SERVICE.md).

Verify foundation health and readiness:

```bash
curl http://localhost:8092/actuator/health
curl http://localhost:8092/actuator/health/readiness
```

Readiness includes the database health indicator and must not report `UP` when the Calendar
database is unavailable. Only `health` and `info` actuator endpoints are exposed; environment,
beans, mappings, and configuration properties are not exposed.

For staging smoke testing, confirm startup logs contain the configured service identity,
successful Flyway initialization, and Eureka registration; then verify `/actuator/health`,
`/actuator/health/readiness`, an unauthenticated protected request returns `401`, and a valid
JWT with an insufficient role returns `403`. Do not include tokens or credentials in evidence.

## 8. Run Tests

Using the available Maven wrapper:

```bash
sh cloud/config-server/mvnw -f pom.xml -pl common-lib test
sh cloud/config-server/mvnw -f pom.xml -pl services/user-service -am test
sh cloud/config-server/mvnw -f pom.xml -pl cloud/api-gateway -am test
```

If Maven is installed globally, you can use equivalent `mvn` commands.

Testing standards and test registers are maintained here:

```txt
docs/testing/README.md
docs/testing/README.md
```

## 9. Useful Docker Commands

Check containers:

```bash
docker compose ps -a
```

View authentication service logs:

```bash
docker logs auth-service --tail 100
```

Restart local infrastructure cleanly:

```bash
docker compose down -v
docker compose up -d
```

Use `down -v` carefully because it deletes local Docker volumes for this compose project.

## 10. Security Rules

Do not commit:

```txt
.env
tokens
real client secrets
private keys
production signing keys
local database dumps with sensitive data
```

Safe to commit:

```txt
.env.example
docs/*.md
```
