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
}
