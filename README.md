# 🎓 Mentify Backend

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Microservices](https://img.shields.io/badge/Architecture-Microservices-blue)](https://microservices.io/)
[![Security](https://img.shields.io/badge/Security-Keycloak-red)](https://www.keycloak.org/)
[![Messaging](https://img.shields.io/badge/Messaging-Kafka-black)](https://kafka.apache.org/)

Mentify is a production-grade, AI-powered Learning Management System (LMS) backend. Built on a modular Spring Boot microservices architecture, it provides a scalable and secure foundation for modern educational platforms, featuring event-driven communication, distributed configuration, and deep integration with LLMs for intelligent learning assistance.

---

## 🏗️ Architecture Overview

Mentify follows a **Cloud-Native Microservices** pattern, leveraging the Spring Cloud ecosystem for infrastructure and Apache Kafka for asynchronous inter-service communication.

### In-Depth System Design

```text
                                       +-------------------------------------------------------+
                                       |                   External Clients                    |
                                       |          (Web Portal, Mobile App, API Users)          |
                                       +---------------------------+---------------------------+
                                                                   |
                                                                   v [HTTPS/TLS]
      +-----------------------+                      +-------------+-------------+
      |  Keycloak (Identity)  |<-------------------->|       Spring Cloud Gateway| (API Gateway)
      |  - OIDC / OAuth2      | [Auth Verification]  | - Auth, Routing, Rate Limit |
      |  - JWT Issuance       |                      | - CORS, Central Entry Point |
      +-----------------------+                      +-------------+-------------+
                                                                   |
            +-----------------------+------------------------------+---------------------------+
            |                       |                              |                           |
    +-------v-------+       +-------v-------+              +-------v-------+           +-------v-------+
    | User Service  |       | Course Service|              | AI Service    |           | Payment Serv. |
    | (Profile/RBAC)|       | (Modules/Lsns)|              | (OpenAI/Tools)|           | (Stripe/Pay)  |
    +-------+-------+       +-------+-------+              +-------+-------+           +-------+-------+
            |                       |                              |                           |
            |           [Inter-Service Communication via Spring Cloud OpenFeign]               |
            |                       |                              |                           |
            +-----------------------+--------------+---------------+---------------------------+
                                                   |
                                     +-------------v-------------+
                                     |  Service Registry (Eureka)| (Service Discovery)
                                     |  Config Server (Spring)   | (Distributed Config)
                                     +-------------+-------------+
                                                   |
                                     +-------------v-------------+
                                     |       Apache Kafka        | (Distributed Event Bus)
                                     |  - Notification Events    | (Asynchronous Flow)
                                     |  - Audit & Analytics      |
                                     +-------------+-------------+
                                                   |
            +-----------------------+--------------+---------------+---------------------------+
            |                       |                              |                           |
    +-------v-------+       +-------v-------+              +-------v-------+           +-------v-------+
    | Notification  |       | Attendance    |              | Exam Service  |           | Material Serv.|
    | (Email/SMS)   |       | (Tracking)    |              | (Proctoring)  |           | (S3/Storage)  |
    +---------------+       +---------------+              +---------------+           +---------------+
```

---

## 🛠️ Tech Stack & Ecosystem

### Core Frameworks
*   **Java 17 (LTS)**: Core programming language.
*   **Spring Boot 3.x**: Service foundation.
*   **Spring Cloud (2022.x/2023.x)**:
    *   **Netflix Eureka**: Service Discovery & Registry.
    *   **Spring Cloud Gateway**: Centralized entry point & Security filter (WebFlux).
    *   **Spring Cloud Config**: Centralized externalized configuration management.
    *   **Spring Cloud OpenFeign**: Declarative REST client for inter-service communication.

### Data & Messaging
*   **PostgreSQL**: Primary relational database for transactional consistency.
*   **MongoDB**: Document storage for unstructured content (AI logs, messaging).
*   **Apache Kafka**: Event streaming platform for decoupled service interaction.
*   **Flyway**: Database versioning and migrations.

### Security & AI
*   **Keycloak**: Enterprise-grade Identity and Access Management (OIDC/OAuth2).
*   **OpenAI API**: Powering intelligent tutor chat, grounded data analysis, and quiz generation.
*   **Apache POI / PDFBox**: For document text extraction and content ingestion.

---

## 📂 Microservices Catalog

Mentify is composed of several specialized services, each responsible for a specific domain:

| Service | Description | Port | Database |
| :--- | :--- | :--- | :--- |
| **`api-gateway`** | Central entry point, JWT validation, and request routing. | `8080` | - |
| **`service-registry`** | Eureka server for service registration and discovery. | `8761` | - |
| **`config-server`** | Manages environment-specific configurations via Git/Native. | `8888` | - |
| **`user-service`** | User profiles, identity syncing, and RBAC management. | `8081` | PostgreSQL |
| **`course-service`** | Management of courses, modules, lessons, and content hierarchy. | `8082` | PostgreSQL |
| **`attendance-service`** | Student attendance tracking and reporting. | `8083` | PostgreSQL |
| **`material-service`** | File storage management (S3/Local) and material metadata. | `8084` | PostgreSQL |
| **`assignment-service`** | Assignment creation, submission tracking, and grading logic. | `8085` | PostgreSQL |
| **`communication-service`**| Internal messaging, forum discussions, and announcements. | `8086` | PostgreSQL |
| **`exam-service`** | Online examination engine with timer and proctoring support. | `8087` | PostgreSQL |
| **`payment-service`** | Integration with payment gateways (Stripe) for enrollment. | `8089` | PostgreSQL |
| **`ai-service`** | OpenAI integration, grounded tool analysis, and quiz generation. | `8090` | MongoDB |
| **`calender-service`** | Event scheduling and synchronization across services. | `8092` | PostgreSQL |
| **`notification-service`**| Kafka-driven engine for Email, SMS, and in-app alerts. | `8093` | PostgreSQL |
| **`entrollment-service`** | Handles course subscriptions and student-course mapping. | `8094` | PostgreSQL |

---

## 🔒 Security & RBAC Model

Mentify implements a multi-layered security architecture:

1.  **Gateway Security**: `api-gateway` uses Spring Security WebFlux to validate JWT signatures issued by Keycloak. It handles CORS and public/private route filtering.
2.  **Service-Level Security**: Every microservice leverages a shared `SecurityConfig` (from `common-lib`) which:
    *   Decodes Keycloak-specific JWT claims.
    *   Maps realm/client roles to Spring Security `GrantedAuthority`.
    *   Enforces method-level security using `@PreAuthorize("hasRole('TEACHER')")`.
3.  **Inter-Service Auth**: Internal communication via Feign clients carries the user's JWT token, ensuring that the security context is propagated across service boundaries.
4.  **Resource Ownership**: Specialized authorization services (e.g., `MaterialAuthorizationServiceImpl`) verify that users only access resources they own or are enrolled in.

---

## 🤖 AI & Intelligent Features

The **AI Service** provides a "Grounded Tool" architecture:
*   **Tutor Chat**: An interactive chatbot for teachers and admins that uses context-aware conversation history.
*   **Grounded Tool Analysis**: Instead of raw LLM generation, the AI uses authorized backend tools (e.g., `TodayQuizPerformanceTool`) to fetch real-time data from other services before summarizing it. This prevents hallucinations and ensures data accuracy.
*   **AI Quiz Generation**: Automatically generates MCQ quizzes based on course materials and lessons.
*   **Content Guardrails**: All AI interactions pass through a `UsageGuard` and `ContentGuard` to ensure policy compliance and quota management.

---

## 📨 Event-Driven Communication

Mentify uses an **AOP-driven event system** to maintain service decoupling:

*   **Producer Side**: Services annotate controller methods with `@NotifyOnSuccess(type = "COURSE_CREATED", ...)`. A shared Aspect (`NotificationOnSuccessAspect`) in `common-lib` intercepts the successful response and publishes a `NotificationEvent` to Kafka.
*   **Message Broker**: Apache Kafka acts as the distributed event bus, ensuring high availability and persistence of events.
*   **Consumer Side**: The `notification-service` consumes events from the `mentify.notification-events` topic and routes them to the appropriate channel (Email via SMTP, SMS, or in-app notifications).

---

## 🚀 Getting Started

### Prerequisites
*   **Java 17+**
*   **Docker & Docker Compose**
*   **Maven 3.8+**

### 1. Environment Configuration
Clone the repository and initialize the environment file:
```bash
git clone https://github.com/your-org/Mentify-backend.git
cd Mentify-backend
cp .env.example .env
```
*Note: Ensure you set valid credentials for Keycloak and OpenAI in the `.env` file.*

### 2. Infrastructure Startup
Mentify uses Docker profiles to manage resources efficiently:

```bash
# Start Core Infrastructure (Keycloak & Keycloak DB)
docker compose up -d

# Start Application Databases (PostgreSQL for all microservices)
docker compose --profile app-db up -d

# Start Messaging & Notifications (Kafka & Mailpit)
docker compose --profile notifications up -d
```

### 3. Service Execution Order
To ensure proper discovery and configuration, start services in this sequence:
1.  **`service-registry`**: Wait for the dashboard at `http://localhost:8761`.
2.  **`config-server`**: Verify configuration is served at `http://localhost:8888`.
3.  **`api-gateway`**: The central entry point for all API requests.
4.  **`common-lib`**: Run `mvn install` to make shared components available.
5.  **Business Services**: Start `user-service`, `course-service`, etc.

---

## 🛠️ Development Guidelines

### Project Structure
*   `cloud/`: Infrastructure services (Gateway, Config, Registry).
*   `services/`: Business-logic microservices.
*   `common-lib/`: Shared DTOs, Exception handlers, Security, and Kafka Aspect.
*   `docker/`: Database initialization scripts and Docker Compose configurations.
*   `docs/`: Detailed technical guides and integration specs.

### Useful Commands
*   **Build Project**: `mvn clean install -DskipTests`
*   **Run Single Service**: `mvn -pl services/user-service spring-boot:run`
*   **Check Health**: All services expose `/actuator/health` and `/actuator/health/readiness`.

---

## 📖 Documentation Index

For deeper technical details, refer to the following guides:
*   📘 [Project Setup & Installation](docs/PROJECT_SETUP.md)
*   🔐 [Keycloak & Security Configuration](docs/KEYCLOAK_SETUP.md)
*   📡 [Kafka & Notification System Guide](docs/NOTIFICATION_SERVICE.md)
*   🧪 [Testing Standards & Quality Gates](docs/testing/README.md)
*   🔄 [Authentication & Session Flows](docs/AUTHENTICATION_SESSION_FLOW.md)

---

## ⚖️ License

This project is **Proprietary and Confidential**. All rights reserved. Unauthorized copying or distribution of this software is strictly prohibited.
