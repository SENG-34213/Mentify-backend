package com.mentify.material.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "material.storage.s3")
public record S3Properties(
        String bucket,
        String region
) {
}
