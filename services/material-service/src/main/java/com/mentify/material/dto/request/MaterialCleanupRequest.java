package com.mentify.material.dto.request;

public record MaterialCleanupRequest(
        Boolean dryRun,
        Integer deletedRetentionDays,
        Integer incompleteUploadAgeHours
) {
}
