package com.mentify.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private long refreshExpiresIn;
    private String scope;
    private Instant issuedAt;
    private Profile profile;

    @Data
    @Builder
    public static class Profile {
        private UUID userId;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private String accountStatus;
        private boolean emailVerified;
        private boolean profileComplete;
    }
}
