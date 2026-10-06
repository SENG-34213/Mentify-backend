package com.mentify.exam.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private Flyway flyway;

    @Test
    void baselineMigrationIsApplied() {
        assertTrue(flyway.info().applied().length >= 1);
        assertEquals("1", flyway.info().current().getVersion().getVersion());
    }
}
