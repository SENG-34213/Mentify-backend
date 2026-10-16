package com.mentify.ai.config;

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
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false"
        }
)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void websocketHandshakeEndpointIsPublicForStompConnectAuthentication() throws Exception {
        mockMvc.perform(get("/ws/ai"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRestEndpointStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/protected-resource"))
                .andExpect(status().isUnauthorized());
    }

    @SpringBootApplication(
            exclude = {
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class
            }
    )
    @Import({SecurityConfig.class, TestController.class})
    static class TestApplication {
    }

    @RestController
    static class TestController {

        @GetMapping("/ws/ai")
        Map<String, String> websocketHandshakeProbe() {
            return Map.of("status", "ok");
        }

        @GetMapping("/protected-resource")
        Map<String, String> protectedResource() {
            return Map.of("status", "protected");
        }
    }
}
