package com.mentify.exam.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class MarksEntryResponse {
    UUID examId;
    int updatedCount;
    List<ExamResultResponse> results;
}