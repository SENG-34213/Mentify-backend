package com.mentify.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SecurityConfigTest.TestApplication.class,
        properties = "mentify.security.enabled=true"
)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void givenNoToken_whenCallingConfiguredPublicEndpoint_thenReturnsOk() throws Exception {
        // Arrange
        var request = get("/api/v1/auth/public-test");

        // Act and Assert
        mockMvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void givenNoToken_whenCallingProtectedEndpoint_thenReturnsUnauthorized() throws Exception {
        // Arrange
        var request = get("/protected-resource");

        // Act and Assert
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    @SpringBootApplication(
            scanBasePackageClasses = SecurityConfig.class,
            exclude = {
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class
            }
    )
    @Import(TestController.class)
    static class TestApplication {
    }

    @RestController
    static class TestController {

        @GetMapping("/api/v1/auth/public-test")
        Map<String, String> publicTest() {
            return Map.of("status", "success");
        }

        @GetMapping("/protected-resource")
        Map<String, String> protectedResource() {
            return Map.of("status", "protected");
        }
    }
}
