package com.mentify.exam.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ExamAttachmentResponse {
    UUID id;
    UUID examId;
    UUID materialId;
    String originalFilename;
    String contentType;
    long fileSize;
    UUID uploadedBy;
    boolean studentVisible;
    boolean active;
    LocalDateTime createdAt;
}
