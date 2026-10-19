package com.mentify.exam.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ExamAttachmentDownloadResponse {
    UUID id;
    UUID examId;
    UUID materialId;
    String originalFilename;
    String contentType;
    long fileSize;
    String downloadUrl;
    Instant expiresAt;
}
