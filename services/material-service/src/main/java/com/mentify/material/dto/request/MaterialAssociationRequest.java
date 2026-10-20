package com.mentify.material.dto.request;

import com.mentify.material.enums.MaterialResourceType;
import com.mentify.material.enums.MaterialVisibility;

import java.util.UUID;

public record MaterialAssociationRequest(
        MaterialResourceType resourceType,
        UUID resourceId,
        UUID courseId,
        UUID ownerId,
        MaterialVisibility visibility
) {
}
