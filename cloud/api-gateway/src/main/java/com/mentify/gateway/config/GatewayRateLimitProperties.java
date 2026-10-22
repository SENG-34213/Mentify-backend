package com.mentify.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mentify.gateway.rate-limit")
public class GatewayRateLimitProperties {

    private boolean enabled = true;
    private boolean trustForwardedHeaders;
    private Policy defaultPolicy = new Policy(20.0, 40, 1);
    private Policy loginPolicy = new Policy(0.5, 5, 1);
    private Policy sessionPolicy = new Policy(1.0, 10, 1);
    private Policy recoveryPolicy = new Policy(0.2, 3, 1);
    private Policy aiPolicy = new Policy(1.0, 4, 1);
    private Policy uploadPolicy = new Policy(2.0, 6, 1);
    private Policy websocketPolicy = new Policy(1.0, 5, 1);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTrustForwardedHeaders() {
        return trustForwardedHeaders;
    }

    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public Policy getDefaultPolicy() {
        return defaultPolicy;
    }

    public void setDefaultPolicy(Policy defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    public Policy getLoginPolicy() {
        return loginPolicy;
    }

    public void setLoginPolicy(Policy loginPolicy) {
        this.loginPolicy = loginPolicy;
    }

    public Policy getRecoveryPolicy() {
        return recoveryPolicy;
    }

    public Policy getSessionPolicy() {
        return sessionPolicy;
    }

    public void setSessionPolicy(Policy sessionPolicy) {
        this.sessionPolicy = sessionPolicy;
    }

    public void setRecoveryPolicy(Policy recoveryPolicy) {
        this.recoveryPolicy = recoveryPolicy;
    }

    public Policy getAiPolicy() {
        return aiPolicy;
    }

    public void setAiPolicy(Policy aiPolicy) {
        this.aiPolicy = aiPolicy;
    }

    public Policy getUploadPolicy() {
        return uploadPolicy;
    }

    public void setUploadPolicy(Policy uploadPolicy) {
        this.uploadPolicy = uploadPolicy;
    }

    public Policy getWebsocketPolicy() {
        return websocketPolicy;
    }

    public void setWebsocketPolicy(Policy websocketPolicy) {
        this.websocketPolicy = websocketPolicy;
    }

    public record Policy(double requestsPerSecond, int burstCapacity, int requestedTokens) {
        public Policy {
            if (requestsPerSecond <= 0 || burstCapacity <= 0 || requestedTokens <= 0
                    || requestedTokens > burstCapacity) {
                throw new IllegalArgumentException("Invalid rate-limit policy");
            }
        }

        public long windowTtlMillis() {
            return Math.max(1_000L, (long) Math.ceil((burstCapacity / requestsPerSecond) * 1_000L) + 1_000L);
        }
    }
}
