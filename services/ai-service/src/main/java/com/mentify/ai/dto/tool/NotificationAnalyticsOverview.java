package com.mentify.ai.dto.tool;

public record NotificationAnalyticsOverview(
        long totalNotifications,
        long unreadNotifications,
        long pendingEmails,
        long sentEmails,
        long failedEmails
) {
}
