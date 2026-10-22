package com.mentify.service.authentication;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthServiceClient {
    @Value("${mentify.auth.service-url:http://localhost:8085}")
    private String serviceUrl;
    @Value("${mentify.auth.internal-token:change-me-internal-auth-token}")
    private String internalToken;

    public void provision(UUID userId, String email, String password) {
        RestClient.builder()
                .baseUrl(serviceUrl)
                .build()
                .post()
                .uri("/api/v1/auth/internal/users")
                .header("X-Internal-Token", internalToken)
                .body(new ProvisionUserRequest(userId, email, password))
                .retrieve()
                .toBodilessEntity();
    }

    public void delete(UUID userId) {
        RestClient.builder()
                .baseUrl(serviceUrl)
                .build()
                .delete()
                .uri("/api/v1/auth/internal/users/{userId}", userId)
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .toBodilessEntity();
    }

    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        RestClient.builder()
                .baseUrl(serviceUrl)
                .build()
                .post()
                .uri("/api/v1/auth/internal/users/{userId}/password", userId)
                .header("X-Internal-Token", internalToken)
                .body(new PasswordChangeRequest(currentPassword, newPassword))
                .retrieve()
                .toBodilessEntity();
    }

    public record ProvisionUserRequest(UUID userId, String email, String password) {
    }

    public record PasswordChangeRequest(String currentPassword, String newPassword) {
    }
}
