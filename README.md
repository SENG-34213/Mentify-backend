# 🎓 Mentify Backend

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Microservices](https://img.shields.io/badge/Architecture-Microservices-blue)](https://microservices.io/)
[![Security](https://img.shields.io/badge/Security-Keycloak-red)](https://www.keycloak.org/)
[![Messaging](https://img.shields.io/badge/Messaging-Kafka-black)](https://kafka.apache.org/)

Mentify is a production-grade, AI-powered Learning Management System (LMS) backend. Built on a modular Spring Boot microservices architecture, it provides a scalable and secure foundation for modern educational platforms, featuring event-driven communication, distributed configuration, and deep integration with LLMs for intelligent learning assistance.

---

## 🏗️ Architecture Overview

Mentify follows a **Cloud-Native Microservices** pattern, leveraging the Spring Cloud ecosystem for infrastructure and Apache Kafka for asynchronous inter-service communication.

### High-Level System Design

```text
                                  +-------------------------------------------------------+
                                  |                   External Clients                    |
                                  |          (Web Portal, Mobile App, API Users)          |
                                  +---------------------------+---------------------------+
                                                              |
                                                              v
      +-----------------------+                 +-------------+-------------+
      |  Keycloak (Identity)  |<--------------->|       Spring Cloud Gateway| (API Gateway)
      |  Auth & JWT Issuance  |                 |      (Auth, Routing, Rate Limit) |
      +-----------------------+                 +-------------+-------------+
                                                              |
            +---------------------+---------------------------+---------------------------+
            |                     |                           |                           |
    +-------v-------+      +------v-------+            +------v-------+            +------v-------+
    | User Service  |      | Course Serv. |            | AI Service   |            | Payment Serv.| ...
    | (Identity)    |      | (Content Management)      | (LLM/RAG/OCR)|            | (Stripe/Pay) |
    +-------+-------+      +------+-------+            +------+-------+            +------+-------+
            |                     |                           |                           |
            +---------------------+-------------+-------------+---------------------------+
                                                |
                                  +-------------v-------------+
                                  |       Apache Kafka        | (Distributed Event Bus)
                                  |  (Notifications, Analytics, Audit Logs) |
                                  +-------------+-------------+
                                                |
                                  +-------------v-------------+
                                  |   Notification Service    |
                                  |   (Email, SMS, Push, Inbox)|
                                  +---------------------------+
```

---

## 🛠️ Tech Stack & Ecosystem

### Core Frameworks
*   **Java 17 (LTS)**: Core programming language.
*   **Spring Boot 3.x**: Service foundation.
*   **Spring Cloud (2022.x/2023.x)**:
    *   **Netflix Eureka**: Service Discovery & Registry.
    *   **Spring Cloud Gateway**: Centralized entry point & Security filter.
    *   **Spring Cloud Config**: Centralized externalized configuration.
    *   **Spring Cloud OpenFeign**: Declarative REST client for inter-service communication.

### Data & Messaging
*   **PostgreSQL**: Primary relational database (Transactional data).
*   **MongoDB**: Document storage for unstructured content (AI logs, communications).
*   **Apache Kafka**: Event streaming platform.
*   **Flyway**: Database versioning and migrations.

### Security & AI
*   **Keycloak**: Open-source Identity and Access Management (OIDC/OAuth2).
*   **OpenAI API**: Integration for RAG (Retrieval-Augmented Generation), Chatbots, and Auto-grading.
*   **Apache POI / PDFBox**: For document text extraction and processing.

---

## 📂 Microservices Catalog

Mentify is composed of several specialized services:

| Service | Description | Port |
| :--- | :--- | :--- |
| **`api-gateway`** | Central entry point, JWT validation, and request routing. | `8080` |
| **`service-registry`** | Eureka server for service registration and discovery. | `8761` |
| **`config-server`** | Manages environment-specific configurations via Git/Native. | `8888` |
| **`user-service`** | User profiles, identity syncing, and RBAC management. | `8081` |
| **`course-service`** | Management of courses, modules, lessons, and content hierarchy. | `8082` |
| **`ai-service`** | OpenAI integration, document OCR, quiz generation, and RAG. | `8090` |
| **`notification-service`**| Kafka-driven engine for Email, SMS, and in-app alerts. | `8093` |
| **`material-service`** | File storage management (S3/Local) and material metadata. | `8084` |
| **`assignment-service`** | Assignment creation, submission tracking, and grading logic. | `8085` |
| **`exam-service`** | Online examination engine with timer and proctoring support. | `8087` |
| **`attendance-service`** | Student attendance tracking and reporting. | `8083` |
| **`communication-service`**| Internal messaging, forum discussions, and announcements. | `8086` |
| **`payment-service`** | Integration with payment gateways for course enrollment. | `8089` |

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
*Edit `.env` to provide your `OPENAI_API_KEY` and other sensitive credentials.*

### 2. Infrastructure Startup
Mentify uses Docker profiles to manage resources. Start the required infrastructure:

```bash
# Start Core (Keycloak & its DB)
docker compose up -d

# Start Application Databases (PostgreSQL for all microservices)
docker compose --profile app-db up -d

# Start Messaging & Notifications (Kafka & Mailpit)
docker compose --profile notifications up -d
```

### 3. Service Execution Order
For local development, services should be started in the following sequence to ensure proper registration and config retrieval:
1.  **`service-registry`** (Wait for Dashboard at `http://localhost:8761`)
2.  **`config-server`** (Verify config at `http://localhost:8888/user-service/default`)
3.  **`api-gateway`**
4.  **`common-lib`** (Run `mvn install` first as it's a dependency for others)
5.  **Business Services** (e.g., `user-service`, `course-service`)

---

## 🔒 Security Model

### Authentication & Authorization
*   **Identity Provider**: Keycloak manages all users, roles, and client credentials.
*   **JWT Flow**: Gateway validates the JWT signature. Services use `common-lib`'s `SecurityConfig` to decode roles (`ROLE_STUDENT`, `ROLE_TEACHER`, `ROLE_ADMIN`) from the token.
*   **Inter-service Auth**: Secure communication via Feign clients using JWT relay or client credentials.

### RBAC Implementation
Access is controlled at the method level using `@PreAuthorize`:
```java
@PreAuthorize("hasRole('TEACHER')")
public CourseResponse createCourse(CourseRequest request) { ... }
```

---

## 🤖 AI & Intelligent Features

The **AI Service** is the brain of Mentify's smart features:
*   **RAG (Retrieval-Augmented Generation)**: Ingests lesson materials (PDF/Docx) into a vector-searchable context for the OpenAI model to provide accurate, material-specific answers to students.
*   **Auto-Quiz Generation**: Automatically generates multiple-choice questions based on lesson content.
*   **AI Grading**: Assists teachers by providing initial sentiment and accuracy analysis on student submissions.

---

## 📨 Event-Driven Communication

Mentify uses an "Event-Carrying State Transfer" pattern:
*   **Notification Triggers**: Services annotate methods with `@NotifyOnSuccess(topic = "user-events")`.
*   **Kafka Producer**: `common-lib` contains an AOP aspect that automatically publishes events to Kafka after successful database transactions.
*   **Kafka Consumer**: `notification-service` listens to topics and routes messages to the appropriate delivery channel (Email/SMS).

---

## 🛠️ Development Guidelines

### Project Structure
*   `cloud/`: Infrastructure-level Spring Boot apps.
*   `services/`: Core business microservices.
*   `common-lib/`: Shared DTOs, Exception handlers, Security, and Kafka configs.
*   `docker/`: Database initialization scripts and Dockerfiles.
*   `docs/`: In-depth guides for specific features (Keycloak, Kafka, etc.).

### Common Commands
*   **Build all**: `mvn clean install -DskipTests`
*   **Run a specific service**: `mvn -pl services/user-service spring-boot:run`
*   **Database Migrations**: Handled automatically by Flyway on startup.

---

## 📖 Documentation Index

For deeper dives, refer to our specialized documentation:
*   📘 [Detailed Project Setup](docs/PROJECT_SETUP.md)
*   🔐 [Keycloak & Security Configuration](docs/KEYCLOAK_SETUP.md)
*   📡 [Notification Service & Kafka Guide](docs/NOTIFICATION_SERVICE.md)
*   🧪 [Testing Standards & Coverage](docs/testing/README.md)
*   🔄 [Authentication & Session Flows](docs/AUTHENTICATION_SESSION_FLOW.md)

---

## ⚖️ License

This project is **Proprietary and Confidential**. All rights reserved. Unauthorized copying or distribution of this software is strictly prohibited.
