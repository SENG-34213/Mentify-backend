package com.mentify.assignment.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MaterialLookupResponse(
        UUID id,
        String title,
        String description,
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
