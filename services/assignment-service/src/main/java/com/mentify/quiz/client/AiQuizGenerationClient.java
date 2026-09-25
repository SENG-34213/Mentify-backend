package com.mentify.quiz.client;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.dto.AiGeneratedQuizDraftResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "ai-service", path = "/api/ai/quizzes")
public interface AiQuizGenerationClient {

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<AiGeneratedQuizDraftResponse> generateQuiz(
            @RequestPart("file") MultipartFile file,
            @RequestParam("courseId") String courseId,
            @RequestParam("questionCount") String questionCount,
            @RequestParam("difficulty") String difficulty,
            @RequestParam("questionType") String questionType,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    );
}
