package com.mentify.communication.dto.response;

import java.time.Instant;

public record AttachmentDownloadResponse(
        String originalFilename,
        String contentType,
        Long fileSize,
        String downloadUrl,
        Instant expiresAt
) {
}
