package com.mentify.material.dto.request;

import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record MaterialSearchCriteria(
        String search,
        MaterialType type,
        MaterialPurpose purpose,
        MaterialStatus status,
        UUID uploadedBy,
        UUID courseId,
        UUID moduleId,
        UUID lessonId,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        Set<UUID> materialIds
) {
    public MaterialSearchCriteria withResolvedScope(UUID resolvedCourseId, Set<UUID> resolvedMaterialIds) {
        return new MaterialSearchCriteria(
                search,
                type,
                purpose,
                status,
                uploadedBy,
                resolvedCourseId,
                moduleId,
                lessonId,
                createdFrom,
                createdTo,
                resolvedMaterialIds
        );
    }
}
