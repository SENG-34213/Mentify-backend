package com.mentify.material.dto.response;

public record S3FileResult(
        byte[] content,
        String contentType,
        long contentLength
) {
}
