package com.mentify.client;

import com.mentify.client.dto.QuizGradebookResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "assignment-service", contextId = "quizGradebookClient", path = "/api/v1")
public interface QuizGradebookClient {

    @GetMapping("/teacher/gradebook/quizzes")
    ApiResponse<List<QuizGradebookResponse>> getQuizzes(
            @RequestParam UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
