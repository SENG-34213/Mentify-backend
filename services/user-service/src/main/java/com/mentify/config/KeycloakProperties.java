package com.mentify.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    @NotBlank
    private String serverUrl;

    @NotBlank
    private String realm;

    @NotBlank
    private String adminClientId;

    @NotBlank
    private String adminClientSecret;

    @NotBlank
    private String authClientId;

    @NotBlank
    private String authClientSecret;

    private boolean includeRefreshTokenInLoginResponse = true;

    private BootstrapSuperAdmin bootstrapSuperAdmin = new BootstrapSuperAdmin();

    @Data
    public static class BootstrapSuperAdmin {
        private boolean enabled;
        private String email;
        private String password;
        private String firstName = "Super";
        private String lastName = "Admin";
        private boolean resetPasswordOnStartup = true;
        private int retryAttempts = 10;
        private long retryDelayMillis = 2000;
    }
}
