package com.mentify.ai.dto.tool;

public record CommunicationAnalyticsOverview(
        long totalGroups,
        long activeGroups,
        long archivedGroups,
        long totalMessages,
        long activeMessages,
        long deletedMessages
) {
}
