package com.mentify.client.dto;

import java.util.UUID;

public record MaterialLookupResponse(
        UUID id,
        String title,
        String type,
        UUID courseId,
        UUID uploadedBy
) {
    public MaterialLookupResponse(UUID id, String title, String type, UUID courseId) {
        this(id, title, type, courseId, null);
    }
}
