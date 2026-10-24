# Build all Mentify services once, then reuse the same runtime image.
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY pom.xml .
COPY common-lib common-lib
COPY cloud cloud
COPY services services

RUN mvn \
    -B \
    -DskipTests \
    -Djacoco.skip=true \
    package

FROM eclipse-temurin:17-jre

WORKDIR /opt/mentify

COPY --from=builder \
    /workspace/cloud/api-gateway/target/api-gateway-*.jar \
    /opt/mentify/api-gateway.jar
COPY --from=builder \
    /workspace/cloud/config-server/target/config-server-*.jar \
    /opt/mentify/config-server.jar
COPY --from=builder \
    /workspace/cloud/service-registry/target/service-registry-*.jar \
    /opt/mentify/service-registry.jar

COPY --from=builder \
    /workspace/services/ai-service/target/ai-service-*.jar \
    /opt/mentify/ai-service.jar
COPY --from=builder \
    /workspace/services/analytics-service/target/analytics-service-*.jar \
    /opt/mentify/analytics-service.jar
COPY --from=builder \
    /workspace/services/assignment-service/target/assignment-service-*.jar \
    /opt/mentify/assignment-service.jar
COPY --from=builder \
    /workspace/services/attendance-service/target/attendance-service-*.jar \
    /opt/mentify/attendance-service.jar
COPY --from=builder \
    /workspace/services/auth-service/target/auth-service-*.jar \
    /opt/mentify/auth-service.jar
COPY --from=builder \
    /workspace/services/calender-service/target/calender-service-*.jar \
    /opt/mentify/calender-service.jar
COPY --from=builder \
    /workspace/services/communication-service/target/communication-service-*.jar \
    /opt/mentify/communication-service.jar
COPY --from=builder \
    /workspace/services/course-service/target/course-service-*.jar \
    /opt/mentify/course-service.jar
COPY --from=builder \
    /workspace/services/entrollment-service/target/entrollment-service-*.jar \
    /opt/mentify/entrollment-service.jar
COPY --from=builder \
    /workspace/services/exam-service/target/exam-service-*.jar \
    /opt/mentify/exam-service.jar
COPY --from=builder \
    /workspace/services/material-service/target/material-service-*.jar \
    /opt/mentify/material-service.jar
COPY --from=builder \
    /workspace/services/notification-service/target/notification-service-*.jar \
    /opt/mentify/notification-service.jar
COPY --from=builder \
    /workspace/services/payment-service/target/payment-service-*.jar \
    /opt/mentify/payment-service.jar
COPY --from=builder \
    /workspace/services/user-service/target/user-service-*.jar \
    /opt/mentify/user-service.jar

RUN useradd \
    --system \
    --create-home \
    --home-dir /home/mentify \
    mentify \
    && chown -R mentify:mentify /opt/mentify
USER mentify

ENV JAVA_OPTS="-XX:MaxRAMPercentage=55 \
    -XX:InitialRAMPercentage=15 \
    -Djava.security.egd=file:/dev/./urandom"
ENV SERVICE_JAR=api-gateway.jar

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/mentify/$SERVICE_JAR"]
