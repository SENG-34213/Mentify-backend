package com.mentify.communication.dto.response;

public record CommunicationAnalyticsOverview(
        long totalGroups,
        long activeGroups,
        long archivedGroups,
        long totalMessages,
        long activeMessages,
        long deletedMessages
) {
}
