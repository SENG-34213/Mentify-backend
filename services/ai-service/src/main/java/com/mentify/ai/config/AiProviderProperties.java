package com.mentify.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiProviderProperties {
    private ProviderConfig provider = new ProviderConfig();
    private GeminiConfig gemini = new GeminiConfig();
    private OpenAIConfig openai = new OpenAIConfig();
    private HttpConfig http = new HttpConfig();
    private RetryConfig retry = new RetryConfig();
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    private GuardrailConfig guardrails = new GuardrailConfig();

    @Data
    public static class ProviderConfig {
        private String name;
        private String apiKey;
        private String model;
    }

    @Data
    public static class GeminiConfig {
        private String apiKey;
        private String model;
        private CostConfig cost = new CostConfig();
    }

    @Data
    public static class OpenAIConfig {
        private String apiKey;
        private String model;
        private CostConfig cost = new CostConfig();
    }

    @Data
    public static class CostConfig {
        private BigDecimal inputTokenCostPerMillion;
        private BigDecimal outputTokenCostPerMillion;
    }

    @Data
    public static class HttpConfig {
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(60);
    }

    @Data
    public static class RetryConfig {
        private boolean enabled = true;
        private int maxAttempts = 3;
        private Duration backoff = Duration.ofMillis(500);
    }

    @Data
    public static class CircuitBreakerConfig {
        private boolean enabled = true;
        private int failureThreshold = 5;
        private Duration openDuration = Duration.ofSeconds(30);
    }

    @Data
    public static class GuardrailConfig {
        private int maxRequestsPerMinute = 10;
        private int maxRequestsPerDay = 100;
        private int maxInputCharacters = 12000;
        private boolean promptInjectionDetectionEnabled = true;
        private boolean contentFilteringEnabled = true;
        private List<String> blockedTerms = new ArrayList<>();
    }
}
