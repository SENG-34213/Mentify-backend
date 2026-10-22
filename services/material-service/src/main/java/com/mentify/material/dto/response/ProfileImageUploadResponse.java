package com.mentify.material.dto.response;

public record ProfileImageUploadResponse(
        String storageKey,
        String contentType
) {
}
