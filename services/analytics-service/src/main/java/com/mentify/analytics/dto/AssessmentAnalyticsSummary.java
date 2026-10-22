package com.mentify.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssessmentAnalyticsSummary(
        BigDecimal completionRate,
        BigDecimal averageScore,
        BigDecimal missingRate
) {
}
