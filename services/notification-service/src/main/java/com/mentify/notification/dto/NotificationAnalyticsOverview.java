package com.mentify.notification.dto;

public record NotificationAnalyticsOverview(
        long totalNotifications,
        long unreadNotifications,
        long pendingEmails,
        long sentEmails,
        long failedEmails
) {
}
