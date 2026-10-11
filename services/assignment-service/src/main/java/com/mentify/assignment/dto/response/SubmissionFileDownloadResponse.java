package com.mentify.assignment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionFileDownloadResponse {
    private UUID id;
    private UUID submissionId;
    private UUID materialId;
    private String originalFilename;
    private String contentType;
    private long fileSize;
    private String downloadUrl;
    private Instant expiresAt;
    private byte[] content;
}
