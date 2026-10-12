package com.mentify.material.dto.response;

import java.util.UUID;

public record MaterialCleanupFailureResponse(
        UUID materialId,
        String storageKeyHash,
        String operation,
        String reason
) {
}
