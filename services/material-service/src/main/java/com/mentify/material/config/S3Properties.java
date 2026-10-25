package com.mentify.material.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;

@ConfigurationProperties(prefix = "material.storage.s3")
public record S3Properties(
        String bucket,
        String region,
        Duration downloadUrlExpiration,
        String profile,
        String accessKey,
        String secretKey,
        String sessionToken,
        String endpoint
) {
    @ConstructorBinding
    public S3Properties {
    }

    public S3Properties(String bucket, String region, Duration downloadUrlExpiration, String profile) {
        this(bucket, region, downloadUrlExpiration, profile, null, null, null, null);
    }
}
