package com.mentify.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuizDraftResponse {

    private UUID courseId;
    private int questionCount;
    private String difficulty;
    private String questionType;
    private List<GeneratedQuestionResponse> questions;
    private String provider;
    private String model;
    private LocalDateTime generatedAt;
}
