package com.mentify.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;

/**
 * Propagates the caller's bearer token to service-to-service Feign requests.
 *
 * Feign clients may receive the header explicitly, but this fallback is
 * required for dashboard/page requests that invoke a client indirectly.
 */
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

        HttpServletRequest request = servletAttributes.getRequest();
        String authorization = request.getHeader("Authorization");
        if (hasText(authorization)) {
            template.header("Authorization");
            template.header("Authorization", authorization);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
