package com.mentify.calendar.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;
import java.util.List;
import com.mentify.payload.response.ApiResponse;

@FeignClient(name = "entrollment-service", url = "${mentify.clients.enrollment-service.url:}", path = "/api/v1/enrollments")
public interface EnrollmentServiceClient {

    @GetMapping("/students/{studentId}/courses/{courseId}/exists")
    boolean isStudentEnrolledInCourse(
            @PathVariable("studentId") UUID studentId,
            @PathVariable("courseId") UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader);

    @GetMapping("/students/{studentId}/courses")
    ApiResponse<List<UUID>> getActiveCourseIdsForStudent(
            @PathVariable("studentId") UUID studentId,
            @RequestHeader("Authorization") String authorizationHeader);
}
