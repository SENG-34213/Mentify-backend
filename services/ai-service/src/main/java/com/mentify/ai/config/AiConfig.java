package com.mentify.ai.config;
 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
 
@Configuration
public class AiConfig {
 
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
