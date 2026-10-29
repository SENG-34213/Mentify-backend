package com.mentify.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "mentify.redis")
public class RedisFeatureProperties {

    // Redis is enabled explicitly by the deployment/config-server environment.
    // This keeps tests and services that do not run Redis usable by default.
    private boolean enabled = false;
    private String keyPrefix = "mentify:v1";
    private Duration cacheDefaultTtl = Duration.ofMinutes(5);
    private Duration lockDefaultTtl = Duration.ofSeconds(30);
    private Duration idempotencyTtl = Duration.ofDays(1);
}
