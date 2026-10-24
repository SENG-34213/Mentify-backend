package com.mentify.material.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties properties) {
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Properties properties) {
        return S3Presigner.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    private AwsCredentialsProvider credentialsProvider(S3Properties properties) {
        String accessKey = trimToNull(properties.accessKey());
        String secretKey = trimToNull(properties.secretKey());
        String sessionToken = trimToNull(properties.sessionToken());

        if ((accessKey == null) != (secretKey == null)) {
            throw new IllegalStateException("Both AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY must be configured");
        }
        if (accessKey != null) {
            return StaticCredentialsProvider.create(sessionToken == null
                    ? AwsBasicCredentials.create(accessKey, secretKey)
                    : AwsSessionCredentials.create(accessKey, secretKey, sessionToken));
        }
        if (sessionToken != null) {
            throw new IllegalStateException("AWS_SESSION_TOKEN requires AWS access key and secret key");
        }

        DefaultCredentialsProvider.Builder builder = DefaultCredentialsProvider.builder();
        if (properties.profile() != null && !properties.profile().isBlank()) {
            builder.profileName(properties.profile());
        }
        return builder.build();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
