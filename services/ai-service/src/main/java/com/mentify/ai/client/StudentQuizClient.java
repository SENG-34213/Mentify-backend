package com.mentify.ai.client;

import com.mentify.ai.dto.tool.StudentQuizSummary;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "assignment-service", contextId = "aiStudentQuizClient")
public interface StudentQuizClient {

    @GetMapping("/api/courses/{courseId}/quizzes")
    ApiResponse<List<StudentQuizSummary>> getCourseQuizzes(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader);
}
