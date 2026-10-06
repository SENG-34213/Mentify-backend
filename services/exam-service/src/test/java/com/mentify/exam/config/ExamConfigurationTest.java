package com.mentify.exam.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "mentify.clients.course-service.url=http://course.example:8082",
        "mentify.clients.enrollment-service.url=http://enrollment.example:8083"
})
@ActiveProfiles("test")
class ExamConfigurationTest {

    private static final Path CONFIG = Path.of("..", "..", "cloud", "config-server", "src", "main", "resources",
            "configuration", "exam-service.yml");

    @MockBean
    private JwtDecoder jwtDecoder;

    @Value("${mentify.clients.course-service.url}")
    private String courseUrl;

    @Value("${mentify.clients.enrollment-service.url}")
    private String enrollmentUrl;

    @Test
    void clientBaseUrlsAreConfigurable() {
        assertEquals("http://course.example:8082", courseUrl);
        assertEquals("http://enrollment.example:8083", enrollmentUrl);
    }

    @Test
    void configServerFileIsEnvDrivenWithoutHardcodedSecrets() throws Exception {
        String yml = Files.readString(CONFIG);
        for (String placeholder : new String[]{"${EXAM_DB_HOST", "${EXAM_DB_PORT", "${EXAM_DB_NAME",
                "${EXAM_DB_USERNAME", "${EXAM_DB_PASSWORD", "${KEYCLOAK_ISSUER_URI", "${EXAM_SERVICE_PORT",
                "${COURSE_SERVICE_URL", "${ENROLLMENT_SERVICE_URL"}) {
            assertTrue(yml.contains(placeholder), "Missing placeholder " + placeholder);
        }
        assertFalse(yml.contains("jdbc:postgresql://localhost"), "Hardcoded datasource URL");
    }
}
