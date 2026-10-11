package com.mentify.material.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "material.storage.s3")
public record S3Properties(
        String bucket,
        String region,
        Duration downloadUrlExpiration
) {
}
