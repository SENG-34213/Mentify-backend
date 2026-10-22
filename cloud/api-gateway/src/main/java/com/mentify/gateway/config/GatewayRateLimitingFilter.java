package com.mentify.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.List;
import java.util.Locale;

/** Applies endpoint-aware, Redis-backed token-bucket limits at the public gateway boundary. */
@Component
@EnableConfigurationProperties(GatewayRateLimitProperties.class)
public class GatewayRateLimitingFilter implements GlobalFilter, Ordered {

    private static final DefaultRedisScript<List<Long>> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>(
            "local values = redis.call('HMGET', KEYS[1], 'tokens', 'timestamp') "
                    + "local tokens = tonumber(values[1]) "
                    + "local last = tonumber(values[2]) "
                    + "local now = tonumber(ARGV[1]) "
                    + "local rate = tonumber(ARGV[2]) "
                    + "local capacity = tonumber(ARGV[3]) "
                    + "local requested = tonumber(ARGV[4]) "
                    + "if not tokens then tokens = capacity end "
                    + "if not last then last = now end "
                    + "if now > last then tokens = math.min(capacity, tokens + ((now - last) * rate / 1000)) end "
                    + "local allowed = 0 "
                    + "local retry = 0 "
                    + "if tokens >= requested then "
                    + "  tokens = tokens - requested "
                    + "  allowed = 1 "
                    + "else "
                    + "  retry = math.ceil((requested - tokens) * 1000 / rate) "
                    + "end "
                    + "redis.call('HSET', KEYS[1], 'tokens', tostring(tokens), 'timestamp', tostring(now)) "
                    + "redis.call('PEXPIRE', KEYS[1], ARGV[5]) "
                    + "return {allowed, math.floor(tokens), retry}",
            (Class<List<Long>>) (Class<?>) List.class
    );

    private final ReactiveStringRedisTemplate redis;
    private final GatewayRateLimitProperties properties;

    public GatewayRateLimitingFilter(ReactiveStringRedisTemplate redis,
                                     GatewayRateLimitProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled() || !shouldLimit(exchange)) {
            return chain.filter(exchange);
        }

        PolicySelection selection = selectPolicy(exchange);
        Mono<String> identity = selection.ipOnly()
                ? Mono.just(clientAddress(exchange))
                : exchange.getPrincipal()
                .map(Principal::getName)
                .switchIfEmpty(Mono.fromSupplier(() -> clientAddress(exchange)));

        return identity.flatMap(id -> check(selection, id))
                .flatMap(decision -> {
                    addRateLimitHeaders(exchange, selection.policy(), decision);
                    if (decision.allowed()) {
                        return chain.filter(exchange);
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER,
                            String.valueOf(Math.max(1L, (decision.retryAfterMillis() + 999L) / 1_000L)));
                    return exchange.getResponse().setComplete();
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private Mono<Decision> check(PolicySelection selection, String identity) {
        var policy = selection.policy();
        String key = "mentify:rate-limit:" + selection.name() + ":" + identity;
        List<String> args = List.of(
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(policy.requestsPerSecond()),
                String.valueOf(policy.burstCapacity()),
                String.valueOf(policy.requestedTokens()),
                String.valueOf(policy.windowTtlMillis())
        );
        return redis.execute(TOKEN_BUCKET_SCRIPT, List.of(key), args)
                .next()
                .map(result -> new Decision(
                        result.size() > 0 && result.get(0) == 1L,
                        result.size() > 1 ? result.get(1) : 0L,
                        result.size() > 2 ? result.get(2) : 0L
                ))
                .defaultIfEmpty(new Decision(true, policy.burstCapacity(), 0L))
                .onErrorResume(error -> Mono.just(new Decision(true, policy.burstCapacity(), 0L)));
    }

    private PolicySelection selectPolicy(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();
        if ("/api/v1/auth/login".equals(path)) {
            return new PolicySelection("login", properties.getLoginPolicy(), true);
        }
        if ("/api/v1/auth/refresh".equals(path)) {
            return new PolicySelection("session", properties.getSessionPolicy(), true);
        }
        if (path.startsWith("/api/v1/auth/forgot-password")) {
            return new PolicySelection("recovery", properties.getRecoveryPolicy(), true);
        }
        if ("/api/ai/chat".equals(path) || "/api/v1/ai/chat".equals(path)
                || "/api/assignments/quizzes/ai/generate".equals(path)) {
            return new PolicySelection("ai", properties.getAiPolicy(), false);
        }
        if (path.equals("/ws") || path.startsWith("/ws/")) {
            return new PolicySelection("websocket", properties.getWebsocketPolicy(), false);
        }
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            String lowerPath = path.toLowerCase(Locale.ROOT);
            if (lowerPath.contains("/upload") || lowerPath.contains("/attachments")
                    || (method == HttpMethod.POST && lowerPath.equals("/api/v1/materials"))) {
                return new PolicySelection("upload", properties.getUploadPolicy(), false);
            }
        }
        return new PolicySelection("api", properties.getDefaultPolicy(), false);
    }

    private boolean shouldLimit(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();
        return method != HttpMethod.OPTIONS
                && !path.startsWith("/actuator/")
                && !path.startsWith("/v3/api-docs")
                && !path.startsWith("/swagger-ui")
                && (path.startsWith("/api/") || path.startsWith("/ws"));
    }

    private String clientAddress(ServerWebExchange exchange) {
        if (properties.isTrustForwardedHeaders()) {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].trim();
            }
        }
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        return address == null || address.getAddress() == null
                ? "unknown"
                : address.getAddress().getHostAddress();
    }

    private void addRateLimitHeaders(ServerWebExchange exchange,
                                     GatewayRateLimitProperties.Policy policy,
                                     Decision decision) {
        exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(policy.burstCapacity()));
        exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
    }

    private record PolicySelection(String name, GatewayRateLimitProperties.Policy policy, boolean ipOnly) {
    }

    private record Decision(boolean allowed, long remaining, long retryAfterMillis) {
    }
}
