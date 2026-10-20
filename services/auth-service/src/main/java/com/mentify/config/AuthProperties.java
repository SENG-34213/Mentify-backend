package com.mentify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mentify.auth")
public class AuthProperties {
    private String issuer = "http://localhost:8085";
    private String audience = "mentify-api";
    private long accessTokenSeconds = 900;
    private long refreshTokenSeconds = 2592000;
    private String keyId = "mentify-auth-rsa";
    private String privateKeyBase64;
    private String publicKeyBase64;
    private boolean allowEphemeralKeys = true;
    private String userServiceUrl = "http://localhost:8081";
    private String internalToken = "change-me-internal-auth-token";
}
