package com.mentify.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserAnalyticsOverview(
        long totalUsers,
        long students,
        long teachers,
        long admins,
        Map<String, Long> accountStatusMix
) {
}
