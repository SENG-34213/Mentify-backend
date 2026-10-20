package com.mentify.exam.client.dto;

import java.time.Instant;
import java.util.UUID;

public record MaterialDownloadResponse(
        UUID materialId, String downloadUrl, Instant expiresAt
) {}
