package com.mentify.dto;

import com.mentify.enums.AccountStatus;

import java.util.Map;

public record UserAnalyticsOverviewResponse(
        long totalUsers,
        long students,
        long teachers,
        long admins,
        Map<AccountStatus, Long> accountStatusMix
) {
}
