package com.mentify.calendar.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class InternalServiceAuthorization {

    private final String expectedToken;

    public InternalServiceAuthorization(
            @Value("${calendar.integration.internal-service-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    public void requireExamService(String serviceName, String token) {
        requireService("exam-service", serviceName, token);
    }

    public void requireAssignmentService(String serviceName, String token) {
        requireService("assignment-service", serviceName, token);
    }

    private void requireService(String expectedService, String serviceName, String token) {
        if (!expectedService.equals(serviceName)
                || expectedToken == null || expectedToken.isBlank()
                || token == null || !expectedToken.equals(token)) {
            throw new AccessDeniedException("Internal service authorization failed");
        }
    }
}
