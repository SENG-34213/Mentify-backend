package com.mentify.ai.client;

import com.mentify.ai.dto.tool.ActiveEnrollmentRecord;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "entrollment-service", path = "/api/v1/enrollments")
public interface EnrollmentAnalyticsClient {

    @GetMapping("/students/{studentId}/courses")
    ApiResponse<List<UUID>> getMyActiveCourseIds(
            @PathVariable("studentId") UUID studentId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping
    ApiResponse<List<ActiveEnrollmentRecord>> getActiveEnrollments(
            @RequestHeader("Authorization") String authorizationHeader
    );
}
