package com.mentify.analytics.config;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class FeignAuthorizationConfigTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void propagatesCallerAuthorizationHeaderToDownstreamRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer analytics-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        new FeignAuthorizationConfig().authorizationHeaderPropagationInterceptor().apply(template);

        assertThat(template.headers().get("Authorization"))
                .containsExactly("Bearer analytics-token");
    }

    @Test
    void preservesAnExplicitAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer caller-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        template.header("Authorization", "Bearer service-token");
        new FeignAuthorizationConfig().authorizationHeaderPropagationInterceptor().apply(template);

        assertThat(template.headers().get("Authorization"))
                .containsExactly("Bearer service-token");
    }
}
