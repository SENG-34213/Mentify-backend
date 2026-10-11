package com.mentify.material.dto.response;

import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;

import java.time.LocalDateTime;
import java.util.UUID;

public record MaterialResponse(
        UUID id,
        String title,
        String description,
        String originalFilename,
        String storageKey,
        String contentType,
        Long fileSize,
        MaterialType type,
        MaterialPurpose purpose,
        MaterialStatus status,
        UUID uploadedBy,
        UUID courseId,
        String checksum,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
