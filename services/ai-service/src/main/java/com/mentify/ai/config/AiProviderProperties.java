package com.mentify.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiProviderProperties {
    private ProviderConfig provider = new ProviderConfig();
    private GeminiConfig gemini = new GeminiConfig();
    private OpenAIConfig openai = new OpenAIConfig();

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
    }

    @Data
    public static class OpenAIConfig {
        private String apiKey;
        private String model;
    }
}
