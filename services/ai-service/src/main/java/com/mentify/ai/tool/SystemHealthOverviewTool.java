package com.mentify.ai.tool;

import com.mentify.ai.dto.tool.SystemHealthOverview;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks the public actuator health endpoint of one instance per discovered
 * service. The short timeout prevents an unavailable service from blocking
 * the administrator assistant for the normal downstream HTTP timeout.
 */
@Component
@Slf4j
public class SystemHealthOverviewTool implements AiTool<SystemHealthOverview> {

    public static final String TOOL_NAME = "getSystemHealthOverview";
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private AiScopedTtlCache cache = new AiScopedTtlCache();

    public SystemHealthOverviewTool(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public SystemHealthOverview execute(String authorizationHeader) {
        return cache.get(TOOL_NAME, authorizationHeader, CACHE_TTL, this::load);
    }

    private SystemHealthOverview load() {
        List<String> services = discoveryClient.getServices().stream()
                .distinct()
                .sorted()
                .toList();
        List<SystemHealthOverview.ServiceHealth> results = services.stream()
                .map(this::checkService)
                .toList();
        int healthy = (int) results.stream()
                .filter(result -> "UP".equals(result.status()))
                .count();
        return new SystemHealthOverview(
                LocalDateTime.now(),
                results.size(),
                healthy,
                results.size() - healthy,
                results
        );
    }

    private SystemHealthOverview.ServiceHealth checkService(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances == null || instances.isEmpty()) {
            return new SystemHealthOverview.ServiceHealth(serviceName, "UNAVAILABLE", 0,
                    "No service instance is registered");
        }

        ServiceInstance instance = instances.get(0);
        String endpoint = instance.getUri().toString() + "/actuator/health";
        long started = System.nanoTime();
        try {
            Map<String, Object> body = restClient.get()
                    .uri(endpoint)
                    .retrieve()
                    .body(new ParameterizedTypeReference<LinkedHashMap<String, Object>>() {
                    });
            String status = body == null || body.get("status") == null
                    ? "UNKNOWN" : String.valueOf(body.get("status")).toUpperCase();
            return new SystemHealthOverview.ServiceHealth(serviceName, status,
                    elapsedMillis(started), "Health endpoint responded");
        } catch (RuntimeException ex) {
            log.debug("Health check failed service={} endpoint={}", serviceName, endpoint, ex);
            return new SystemHealthOverview.ServiceHealth(serviceName, "UNAVAILABLE",
                    elapsedMillis(started), ex.getClass().getSimpleName());
        }
    }

    private long elapsedMillis(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }
}
