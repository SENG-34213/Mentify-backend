package com.mentify.ai.client;

import com.mentify.ai.dto.tool.StudentCourseProgress;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "course-service", contextId = "aiStudentProgressClient",
        path = "/api/v1/progress")
public interface StudentProgressClient {

    @GetMapping("/courses/{courseId}")
    ApiResponse<StudentCourseProgress> getCourseProgress(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader);
}
