package com.mentify.ai.controller;

import com.mentify.ai.dto.request.QuizGenerationRequest;
import com.mentify.ai.dto.response.GeneratedQuizDraftResponse;
import com.mentify.ai.service.QuizGenerationService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai/quizzes")
@RequiredArgsConstructor
public class QuizGenerationController {

    private final QuizGenerationService quizGenerationService;

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GeneratedQuizDraftResponse>> generateQuiz(
            @RequestPart("file") MultipartFile file,
            @RequestParam UUID courseId,
            @RequestParam Integer questionCount,
            @RequestParam String difficulty,
            @RequestParam String questionType
    ) {
        QuizGenerationRequest request = QuizGenerationRequest.builder()
                .courseId(courseId)
                .questionCount(questionCount)
                .difficulty(difficulty)
                .questionType(questionType)
                .build();
        GeneratedQuizDraftResponse draft = quizGenerationService.generateQuiz(request, file);
        return ResponseEntity.ok(ApiResponse.<GeneratedQuizDraftResponse>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("AI quiz draft generated successfully")
                .data(draft)
                .build());
    }
}
