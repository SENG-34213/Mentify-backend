package com.mentify.client.dto;

import java.util.UUID;

public record MaterialLookupResponse(
        UUID id,
        String title,
        String type,
        UUID courseId,
        UUID uploadedBy,
        String originalFilename,
        String contentType,
        Long fileSize,
        String status
) {
    public MaterialLookupResponse(UUID id, String title, String type, UUID courseId) {
        this(id, title, type, courseId, null, null, null, null, null);
    }
}
