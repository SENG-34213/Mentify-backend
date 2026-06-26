package com.mentify.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@SpringBootTest(
        classes = GatewaySecurityConfigTest.TestApplication.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false"
        }
)
@AutoConfigureWebTestClient
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void givenNoToken_whenCallingGatewayPublicEndpoint_thenReturnsOk() {
        // Arrange
        var request = webTestClient.get().uri("/api/v1/auth/public-test");

        // Act and Assert
        request.exchange()
                .expectStatus().isOk();
    }

    @Test
    void givenNoToken_whenCallingGatewayRefreshEndpoint_thenReturnsOk() {
        // Arrange
        var request = webTestClient.post().uri("/api/v1/auth/refresh");

        // Act and Assert
        request.exchange()
                .expectStatus().isOk();
    }

    @Test
    void givenNoToken_whenCallingGatewayLogoutEndpoint_thenReturnsOk() {
        // Arrange
        var request = webTestClient.post().uri("/api/v1/auth/logout");

        // Act and Assert
        request.exchange()
                .expectStatus().isOk();
    }

    @Test
    void givenNoToken_whenCallingGatewayForgotPasswordEndpoint_thenReturnsOk() {
        // Arrange
        var request = webTestClient.post().uri("/api/v1/auth/forgot-password");

        // Act and Assert
        request.exchange()
                .expectStatus().isOk();
    }

    @Test
    void givenNoToken_whenCallingGatewayProtectedEndpoint_thenReturnsUnauthorized() {
        // Arrange
        var request = webTestClient.get().uri("/protected-gateway-resource");

        // Act and Assert
        request.exchange()
                .expectStatus().isUnauthorized();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({GatewaySecurityConfig.class, TestController.class})
    static class TestApplication {

        @Bean
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.error(new IllegalArgumentException("JWT decoding is not used by anonymous tests"));
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/api/v1/auth/public-test")
        Map<String, String> publicTest() {
            return Map.of("status", "success");
        }

        @PostMapping("/api/v1/auth/refresh")
        Map<String, String> refresh() {
            return Map.of("status", "success");
        }

        @PostMapping("/api/v1/auth/logout")
        Map<String, String> logout() {
            return Map.of("status", "success");
        }

        @PostMapping("/api/v1/auth/forgot-password")
        Map<String, String> forgotPassword() {
            return Map.of("status", "success");
        }

        @GetMapping("/protected-gateway-resource")
        Map<String, String> protectedResource() {
            return Map.of("status", "protected");
        }
    }
}
