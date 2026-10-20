package com.mentify.material.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialConfigurationTest {

    private static final Path CONFIG = Path.of("..", "..", "cloud", "config-server", "src", "main", "resources",
            "configuration", "material-service.yml");

    @Test
    void configServerFileUsesEnvironmentDrivenInfrastructureValues() throws Exception {
        String yml = Files.readString(CONFIG);

        for (String placeholder : new String[]{
                "${MATERIAL_SERVICE_PORT",
                "${MATERIAL_DB_HOST",
                "${MATERIAL_DB_PORT",
                "${MATERIAL_DB_NAME",
                "${MATERIAL_DB_USERNAME",
                "${MATERIAL_DB_PASSWORD",
                "${AUTH_ISSUER_URI",
                "${MATERIAL_S3_BUCKET",
                "${MATERIAL_DOWNLOAD_URL_EXPIRATION",
                "${AWS_REGION",
                "${AWS_DEFAULT_REGION",
                "${AWS_PROFILE",
                "${MATERIAL_MAX_UPLOAD_SIZE"
        }) {
            assertTrue(yml.contains(placeholder), "Missing placeholder " + placeholder);
        }

        assertFalse(yml.contains("access-key"), "AWS access keys must not be configured in source");
        assertFalse(yml.contains("secret-key"), "AWS secret keys must not be configured in source");
    }
}
