package com.mentify.quiz.dto.response;

import com.mentify.quiz.dto.request.CreateQuestionRequest;
import com.mentify.quiz.enums.QuestionType;
import com.mentify.quiz.enums.QuizCreationMethod;
import com.mentify.quiz.enums.QuizGenerationDifficulty;
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
public class AiQuizDraftResponse {

    private UUID courseId;
    private int questionCount;
    private QuizGenerationDifficulty difficulty;
    private QuestionType questionType;
    private QuizCreationMethod creationMethod;
    private List<CreateQuestionRequest> questions;
    private String provider;
    private String model;
    private LocalDateTime generatedAt;
    private boolean saved;
    private boolean published;
}
