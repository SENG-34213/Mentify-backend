package com.mentify.exam.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    void examMigrationIsApplied() {
        assertTrue(Arrays.stream(flyway.info().applied())
                .anyMatch(migration -> migration.getVersion() != null
                        && "2".equals(migration.getVersion().getVersion())));
    }

    @Test
    void examMigrationCreatesCourseAndDateIndexes() throws SQLException {
        Set<String> indexNames = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, "EXAMS", false, false)) {
            while (indexes.next()) {
                indexNames.add(indexes.getString("INDEX_NAME").toLowerCase(Locale.ROOT));
            }
        }

        assertTrue(indexNames.contains("idx_exams_course_id"));
        assertTrue(indexNames.contains("idx_exams_exam_date"));
    }
}
