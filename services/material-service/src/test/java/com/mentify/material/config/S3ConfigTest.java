package com.mentify.material.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class S3ConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(S3Config.class)
            .withPropertyValues(
                    "material.storage.s3.bucket=material-service-test-bucket",
                    "material.storage.s3.region=ap-south-1",
                    "material.storage.s3.download-url-expiration=PT3M"
            );

    @Test
    void createsS3ClientFromConfiguredRegionAndDefaultCredentialChain() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(S3Client.class);
            assertThat(context).hasSingleBean(S3Presigner.class);
            assertThat(context).hasSingleBean(S3Properties.class);
            assertThat(context.getBean(S3Properties.class).bucket()).isEqualTo("material-service-test-bucket");
            assertThat(context.getBean(S3Properties.class).region()).isEqualTo("ap-south-1");
            assertThat(context.getBean(S3Properties.class).downloadUrlExpiration()).isEqualTo(Duration.ofMinutes(3));
        });
    }
}
