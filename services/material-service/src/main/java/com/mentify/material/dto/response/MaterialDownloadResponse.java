package com.mentify.material.dto.response;

import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record MaterialDownloadResponse(
        UUID id,
        String title,
        String description,
        String originalFilename,
        String contentType,
        Long fileSize,
        MaterialType type,
        MaterialPurpose purpose,
        MaterialStatus status,
        UUID uploadedBy,
        String checksum,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String downloadUrl,
        Instant expiresAt
) {
}
