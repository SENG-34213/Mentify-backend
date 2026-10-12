package com.mentify.material.dto.response;

import java.util.List;
import java.util.UUID;

public record MaterialCleanupResponse(
        boolean dryRun,
        int orphanObjectCount,
        int missingObjectCount,
        int failedOrIncompleteCount,
        int purgeableDeletedCount,
        int orphanObjectsDeleted,
        int materialsPurged,
        List<String> orphanObjectKeyHashes,
        List<UUID> missingObjectMaterialIds,
        List<UUID> failedOrIncompleteMaterialIds,
        List<UUID> purgeableDeletedMaterialIds,
        List<MaterialCleanupFailureResponse> failures
) {
}
