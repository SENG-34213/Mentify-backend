package com.mentify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mentify.auth")
public class AuthProperties {
    private String internalToken = "change-me-internal-auth-token";
}
