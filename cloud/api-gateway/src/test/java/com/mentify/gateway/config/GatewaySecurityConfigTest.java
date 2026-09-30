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

import static org.assertj.core.api.Assertions.assertThat;

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
    void givenNoToken_whenCallingAiQuizGenerationEndpoint_thenReturnsOk() {
        // Arrange
        var request = webTestClient.post().uri("/api/assignments/quizzes/ai/generate");

        // Act and Assert
        request.exchange()
                .expectStatus().isOk();
    }

    @Test
    void givenNoToken_whenCallingAiWebSocketHandshakePath_thenReturnsOk() {
        // Arrange
        var request = webTestClient.get().uri("/ws/ai");

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

    @Test
    void givenAllowedFrontendOrigin_whenSendingPreflightRequest_thenReturnsCorsHeaders() {
        webTestClient.options()
                .uri("http://api.mentify.test/protected-gateway-resource")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true")
                .expectHeader().value("Access-Control-Allow-Methods", value -> assertThat(value).contains("GET"))
                .expectHeader().value("Access-Control-Allow-Headers", value -> assertThat(value)
                        .contains("Authorization")
                        .contains("Content-Type"));
    }

    @Test
    void givenAllowedFrontendOrigin_whenCallingPublicEndpoint_thenReturnsCorsHeaders() {
        webTestClient.get()
                .uri("http://api.mentify.test/api/v1/auth/public-test")
                .header("Origin", "http://localhost:5173")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({GatewaySecurityConfig.class, GatewayCorsProperties.class, TestController.class})
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

        @PostMapping("/api/assignments/quizzes/ai/generate")
        Map<String, String> aiQuizGenerate() {
            return Map.of("status", "success");
        }

        @GetMapping("/ws/ai")
        Map<String, String> aiWebSocketHandshakeProbe() {
            return Map.of("status", "success");
        }

        @GetMapping("/protected-gateway-resource")
        Map<String, String> protectedResource() {
            return Map.of("status", "protected");
        }
    }
}
