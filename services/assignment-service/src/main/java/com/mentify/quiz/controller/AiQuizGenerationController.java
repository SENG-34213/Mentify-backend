package com.mentify.quiz.controller;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.dto.response.AiQuizDraftResponse;
import com.mentify.quiz.enums.QuestionType;
import com.mentify.quiz.enums.QuizGenerationDifficulty;
import com.mentify.quiz.service.AiQuizGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AiQuizGenerationController {

    private final AiQuizGenerationService aiQuizGenerationService;

    @PostMapping(value = "/api/assignments/quizzes/ai/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AiQuizDraftResponse>> generateQuizDraft(
            @RequestPart("file") MultipartFile file,
            @RequestParam UUID courseId,
            @RequestParam Integer questionCount,
            @RequestParam QuizGenerationDifficulty difficulty,
            @RequestParam QuestionType questionType,
            @RequestParam(required = false) String userPrompt,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        ApiResponse<AiQuizDraftResponse> response = aiQuizGenerationService.generateDraft(
                file,
                courseId,
                questionCount,
                difficulty,
                questionType,
                userPrompt,
                authorizationHeader
        );
        return new ResponseEntity<>(response, response.getStatus());
    }
}
