package com.mentify.assignment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionFileResponse {
    private UUID id;
    private UUID submissionId;
    private String originalFilename;
    private String contentType;
    private long fileSize;
    private LocalDateTime createdAt;
}
