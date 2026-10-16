package com.mentify.material.client.dto;

import java.util.List;
import java.util.UUID;

public record MaterialScopeLookupResponse(
        UUID courseId,
        List<UUID> materialIds
) {
}
