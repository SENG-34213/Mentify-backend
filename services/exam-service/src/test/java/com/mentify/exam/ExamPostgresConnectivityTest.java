package com.mentify.exam;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Runs against a real PostgreSQL instance; enabled only when EXAM_DB_HOST is provided (CI integration job). */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "EXAM_DB_HOST", matches = ".+")
class ExamPostgresConnectivityTest {

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        String host = System.getenv("EXAM_DB_HOST");
        if (host == null || host.isBlank()) {
            return;
        }
        String port = System.getenv().getOrDefault("EXAM_DB_PORT", "5432");
        String name = System.getenv().getOrDefault("EXAM_DB_NAME", "exam_db");
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + host + ":" + port + "/" + name);
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("EXAM_DB_USERNAME", "postgres"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("EXAM_DB_PASSWORD", ""));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Test
    void connectsToPostgres() {
        assertEquals(1, jdbcTemplate.queryForObject("select 1", Integer.class));
    }
}
