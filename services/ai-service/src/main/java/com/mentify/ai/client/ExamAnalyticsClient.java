package com.mentify.ai.client;

import com.mentify.ai.dto.tool.ExamSummaryToolResult;
import com.mentify.ai.dto.tool.StudentExamResultsToolResult;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "exam-service", path = "/api/v1/exams")
public interface ExamAnalyticsClient {

    @GetMapping("/me")
    ApiResponse<StudentExamResultsToolResult> getMyExamResults(
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/{examId}/summary")
    ApiResponse<ExamSummaryToolResult> getExamSummary(
            @PathVariable("examId") UUID examId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
