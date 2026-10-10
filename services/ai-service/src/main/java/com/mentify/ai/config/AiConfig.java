package com.mentify.ai.config;
 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
 
@Configuration
public class AiConfig {
 
    @Bean
    public RestClient restClient(RestClient.Builder builder, AiProviderProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getHttp().getConnectTimeout());
        requestFactory.setReadTimeout(properties.getHttp().getReadTimeout());

        return builder
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
