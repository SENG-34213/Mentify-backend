package com.mentify.ai.dto.internal;

import java.util.UUID;

public record MaterialDocument(
        UUID id,
        String title,
        String originalFilename,
        String contentType,
        Long fileSize,
        String type,
        String status,
        UUID uploadedBy,
        UUID courseId,
        byte[] content
) {
    public boolean isPdf() {
        return "application/pdf".equalsIgnoreCase(contentType)
                || (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf"));
    }
}
