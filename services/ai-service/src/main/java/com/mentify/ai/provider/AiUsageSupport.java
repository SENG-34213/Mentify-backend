package com.mentify.ai.provider;

import com.mentify.ai.config.AiProviderProperties;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

final class AiUsageSupport {
    private static final BigDecimal TOKENS_PER_MILLION = BigDecimal.valueOf(1_000_000);

    private AiUsageSupport() {
    }

    static Integer intValue(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    static String stringValue(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : value.toString();
    }

    static String firstHeader(HttpHeaders headers, String... names) {
        if (headers == null) {
            return null;
        }
        for (String name : names) {
            String value = headers.getFirst(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    static BigDecimal estimateCost(Integer inputTokens, Integer outputTokens, AiProviderProperties.CostConfig costConfig) {
        if (costConfig == null || inputTokens == null || outputTokens == null) {
            return null;
        }

        BigDecimal inputRate = costConfig.getInputTokenCostPerMillion();
        BigDecimal outputRate = costConfig.getOutputTokenCostPerMillion();
        if (inputRate == null || outputRate == null) {
            return null;
        }

        BigDecimal inputCost = BigDecimal.valueOf(inputTokens).multiply(inputRate);
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens).multiply(outputRate);
        return inputCost.add(outputCost)
                .divide(TOKENS_PER_MILLION, 8, RoundingMode.HALF_UP);
    }
}
