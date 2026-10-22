package com.mentify.client;

import com.mentify.config.AuthProperties;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class UserProfileClient {
    private final RestClient client;
    private final AuthProperties properties;

    public UserProfileClient(AuthProperties properties) {
        this.properties = properties;
        this.client = RestClient.builder()
                .baseUrl(properties.getUserServiceUrl())
                .build();
    }

    public AuthProfile findByEmail(String email) {
        return client.get()
                .uri(uri -> uri
                        .path("/api/v1/internal/users/auth-profile")
                        .queryParam("email", email)
                        .build())
                .header("X-Internal-Token", properties.getInternalToken())
                .retrieve()
                .body(AuthProfile.class);
    }

    public AuthProfile activateInvitedUser(String email) {
        return client.post()
                .uri(uri -> uri
                        .path("/api/v1/internal/users/auth-profile/activate")
                        .queryParam("email", email)
                        .build())
                .header("X-Internal-Token", properties.getInternalToken())
                .retrieve()
                .body(AuthProfile.class);
    }

    public void recordLastLogin(String email) {
        client.post()
                .uri(uri -> uri
                        .path("/api/v1/internal/users/auth-profile/last-login")
                        .queryParam("email", email)
                        .build())
                .header("X-Internal-Token", properties.getInternalToken())
                .retrieve()
                .toBodilessEntity();
    }

    @Data
    public static class AuthProfile {
        private UUID userId;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private String accountStatus;
        private boolean accountNonLocked;
        private boolean emailVerified;
        private boolean profileComplete;
        private boolean active;
    }
}
