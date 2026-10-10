package com.mentify.exam.client;

import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

/** Enrollment Service is the source of truth for course enrollment; the caller's token is forwarded. */
@FeignClient(
        name = "entrollment-service",
        url = "${mentify.clients.enrollment-service.url:}",
        path = "/api/v1/enrollments"
)
public interface EnrollmentServiceClient {

    @GetMapping("/courses/{courseId}/students")
    ApiResponse<List<UUID>> getEnrolledStudentIdsByCourse(
            @PathVariable("courseId") UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader);
}
