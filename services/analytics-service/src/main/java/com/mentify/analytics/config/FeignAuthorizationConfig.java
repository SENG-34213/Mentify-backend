package com.mentify.analytics.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;

/** Propagates the caller's bearer token to downstream analytics providers. */
@Configuration
public class FeignAuthorizationConfig {

    @Bean
    RequestInterceptor authorizationHeaderPropagationInterceptor() {
        return this::propagateAuthorizationHeader;
    }

    private void propagateAuthorizationHeader(RequestTemplate template) {
        Collection<String> existingHeaders = template.headers().get("Authorization");
        if (existingHeaders != null && existingHeaders.stream().anyMatch(this::hasText)) {
            return;
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return;
        }

        String authorization = servletAttributes.getRequest().getHeader("Authorization");
        if (hasText(authorization)) {
            template.header("Authorization", authorization);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
