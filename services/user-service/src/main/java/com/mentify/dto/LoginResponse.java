package com.mentify.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private Long refreshExpiresIn;
    private String scope;
    private Instant issuedAt;
    private UserSummary user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserSummary {
        private UUID id;
        private String keycloakUserId;
        private String email;
        private String firstName;
        private String lastName;
        private Role role;
        private AccountStatus accountStatus;
        private boolean emailVerified;
        private boolean profileComplete;
    }
}
