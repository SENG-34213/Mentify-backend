package com.mentify.quiz.client.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class AiGeneratedQuizDraftResponse {

    private UUID courseId;
    private int questionCount;
    private String difficulty;
    private String questionType;
    private List<AiGeneratedQuestionResponse> questions;
    private String provider;
    private String model;
    private LocalDateTime generatedAt;
}
