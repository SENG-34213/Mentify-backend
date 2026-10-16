package com.mentify.client.dto;

import java.util.UUID;

public record MaterialLookupResponse(
        UUID id,
        String title,
        String type,
        UUID courseId
) {
}
