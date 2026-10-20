package com.mentify.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MaterialUploadResponse(
        UUID id,
        String title,
        String originalFilename,
        String contentType,
        Long fileSize,
        String type,
        String purpose,
        String status,
        UUID uploadedBy,
        UUID courseId,
        String checksum,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
