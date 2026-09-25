package com.mentify.quiz.service;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.response.AiQuizDraftResponse;
import com.mentify.quiz.enums.QuestionType;
import com.mentify.quiz.enums.QuizGenerationDifficulty;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface AiQuizGenerationService {

    ApiResponse<AiQuizDraftResponse> generateDraft(
            MultipartFile file,
            UUID courseId,
            Integer questionCount,
            QuizGenerationDifficulty difficulty,
            QuestionType questionType,
            String authorizationHeader
    );
}
