package com.mentify.material.dto.response;

import java.time.Instant;

public record S3DownloadResult(
        String downloadUrl,
        Instant expiresAt
) {
}
