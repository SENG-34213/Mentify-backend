package com.mentify.client;

import com.mentify.client.dto.ExamDashboardResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "exam-service", path = "/api/v1/exams")
public interface ExamDashboardClient {

    @GetMapping("/courses/{courseId}")
    ApiResponse<List<ExamDashboardResponse>> getExams(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
