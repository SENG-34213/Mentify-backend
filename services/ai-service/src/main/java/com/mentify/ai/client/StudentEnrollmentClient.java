package com.mentify.ai.client;

import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "entrollment-service", contextId = "aiStudentEnrollmentClient",
        path = "/api/v1/enrollments")
public interface StudentEnrollmentClient {

    @GetMapping("/students/{studentId}/courses")
    ApiResponse<List<UUID>> getStudentCourseIds(
            @PathVariable UUID studentId,
            @RequestHeader("Authorization") String authorizationHeader);
}
