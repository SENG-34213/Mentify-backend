package com.mentify.ai.client;

import com.mentify.ai.dto.tool.EnrollmentCourseDetails;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "course-service", contextId = "enrollmentCourseCatalogClient", path = "/api/v1/course")
public interface EnrollmentCourseCatalogClient {

    @GetMapping("/{courseId}/lookup")
    ApiResponse<EnrollmentCourseDetails> getCourseDetails(
            @PathVariable("courseId") UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
