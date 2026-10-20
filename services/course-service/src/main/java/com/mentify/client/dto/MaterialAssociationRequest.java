package com.mentify.client.dto;

import java.util.UUID;

public record MaterialAssociationRequest(
        String resourceType,
        UUID resourceId,
        UUID courseId,
        UUID ownerId,
        String visibility
) {
}
