package com.mentify.ai.service.impl;

import com.mentify.ai.config.AiProviderProperties;
import com.mentify.ai.enums.AiFeatureType;
import com.mentify.ai.exception.AiQuotaExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiUsageGuardServiceImplTest {

    private AiProviderProperties properties;
    private MutableClock clock;
    private AiUsageGuardServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new AiProviderProperties();
        properties.getGuardrails().setMaxRequestsPerMinute(2);
        properties.getGuardrails().setMaxRequestsPerHour(3);
        properties.getGuardrails().setMaxRequestsPerDay(4);
        clock = new MutableClock(Instant.parse("2026-09-25T08:00:00Z"));
        service = new AiUsageGuardServiceImpl(properties, clock);
    }

    @Test
    void blocksRequestsOverMinuteLimitAndAllowsNextMinute() {
        UUID userId = UUID.randomUUID();

        service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION);
        service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION);

        assertThatThrownBy(() -> service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION))
                .isInstanceOf(AiQuotaExceededException.class)
                .hasMessageContaining("minute rate limit exceeded")
                .extracting("retryAfterSeconds")
                .isEqualTo(60L);

        clock.advanceSeconds(60);

        assertThatCode(() -> service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION))
                .doesNotThrowAnyException();
    }

    @Test
    void blocksRequestsOverHourLimit() {
        UUID userId = UUID.randomUUID();
        properties.getGuardrails().setMaxRequestsPerMinute(10);
        properties.getGuardrails().setMaxRequestsPerHour(2);

        service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION);
        service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION);

        assertThatThrownBy(() -> service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION))
                .isInstanceOf(AiQuotaExceededException.class)
                .hasMessageContaining("hour rate limit exceeded")
                .extracting("retryAfterSeconds")
                .isEqualTo(3600L);
    }

    @Test
    void blocksRequestsOverDailyLimit() {
        UUID userId = UUID.randomUUID();
        properties.getGuardrails().setMaxRequestsPerMinute(10);
        properties.getGuardrails().setMaxRequestsPerHour(10);
        properties.getGuardrails().setMaxRequestsPerDay(2);

        service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION);
        service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION);

        assertThatThrownBy(() -> service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION))
                .isInstanceOf(AiQuotaExceededException.class)
                .hasMessageContaining("day rate limit exceeded")
                .extracting("retryAfterSeconds")
                .isEqualTo(57600L);
    }

    @Test
    void tracksFeatureLimitsSeparatelyForSameUser() {
        UUID userId = UUID.randomUUID();

        service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION);
        service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION);

        assertThatCode(() -> service.assertAllowed(userId, AiFeatureType.GENERAL_GENERATION))
                .doesNotThrowAnyException();
    }

    @Test
    void nonPositiveLimitsDisableRateLimitBuckets() {
        UUID userId = UUID.randomUUID();
        properties.getGuardrails().setMaxRequestsPerMinute(0);
        properties.getGuardrails().setMaxRequestsPerHour(0);
        properties.getGuardrails().setMaxRequestsPerDay(0);

        for (int i = 0; i < 20; i++) {
            assertThatCode(() -> service.assertAllowed(userId, AiFeatureType.QUIZ_GENERATION))
                    .doesNotThrowAnyException();
        }
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
