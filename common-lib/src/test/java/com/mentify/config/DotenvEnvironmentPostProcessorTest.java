package com.mentify.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.StandardEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DotenvEnvironmentPostProcessorTest {

    private final String originalUserDir = System.getProperty("user.dir");
    private final DotenvEnvironmentPostProcessor processor = new DotenvEnvironmentPostProcessor();

    @TempDir
    Path tempDir;

    @AfterEach
    void restoreUserDir() {
        System.setProperty("user.dir", originalUserDir);
    }

    @Test
    void loadsDotenvFromCurrentDirectory() throws Exception {
        Files.writeString(tempDir.resolve(".env"), """
                # comment
                MENTIFY_SAMPLE_USERS_ENABLED=true
                MENTIFY_SAMPLE_USERS_PASSWORD="Sample@12345"
                export KEYCLOAK_REALM=mentify
                """);
        System.setProperty("user.dir", tempDir.toString());

        StandardEnvironment environment = new StandardEnvironment();

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("MENTIFY_SAMPLE_USERS_ENABLED")).isEqualTo("true");
        assertThat(environment.getProperty("MENTIFY_SAMPLE_USERS_PASSWORD")).isEqualTo("Sample@12345");
        assertThat(environment.getProperty("KEYCLOAK_REALM")).isEqualTo("mentify");
    }

    @Test
    void searchesParentDirectoriesForDotenv() throws Exception {
        Path serviceDirectory = Files.createDirectories(tempDir.resolve("services/user-service"));
        Files.writeString(tempDir.resolve(".env"), "MENTIFY_SAMPLE_USERS_ENABLED=true\n");
        System.setProperty("user.dir", serviceDirectory.toString());

        StandardEnvironment environment = new StandardEnvironment();

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("MENTIFY_SAMPLE_USERS_ENABLED")).isEqualTo("true");
    }
}
