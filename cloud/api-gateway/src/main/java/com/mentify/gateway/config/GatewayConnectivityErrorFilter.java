package com.mentify.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Prevents Netty/Eureka connectivity details from reaching clients. */
@Component
public class GatewayConnectivityErrorFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper;

    public GatewayConnectivityErrorFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).onErrorResume(error -> {
            if (!isConnectivityFailure(error) || exchange.getResponse().isCommitted()) {
                return Mono.error(error);
            }

            var response = exchange.getResponse();
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", 503);
            body.put("error", "SERVICE_UNAVAILABLE");
            body.put("message", "The requested service is temporarily unavailable. Please try again shortly.");
            body.put("path", exchange.getRequest().getPath().value());

            try {
                byte[] bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
                return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
            } catch (Exception serializationFailure) {
                return Mono.error(serializationFailure);
            }
        });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private boolean isConnectivityFailure(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            String name = current.getClass().getName();
            if (name.contains("ConnectTimeoutException")
                    || name.contains("ConnectException")
                    || name.contains("ReadTimeoutException")
                    || name.contains("TimeoutException")
                    || name.contains("NoSuchElementException")) {
                return true;
            }
        }
        return false;
    }
}
