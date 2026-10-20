package com.mentify.material.dto.response;

import com.mentify.material.enums.MaterialResourceType;
import com.mentify.material.enums.MaterialVisibility;

import java.util.UUID;

public record MaterialAssociationResponse(
        UUID materialId,
        MaterialResourceType resourceType,
        UUID resourceId,
        UUID courseId,
        UUID ownerId,
        MaterialVisibility visibility
) {
}
