package com.mentify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mentify.sample-users")
public class SampleUsersProperties {

    private boolean enabled = false;

    private int usersPerRole = 5;

    private String emailDomain = "mentify.test";

    private String password = "Sample@12345";

    private boolean resetPasswordOnStartup = true;

    private int retryAttempts = 10;

    private long retryDelayMillis = 2000;
}
