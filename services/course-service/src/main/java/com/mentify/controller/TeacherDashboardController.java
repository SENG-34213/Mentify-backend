package com.mentify.controller;

import com.mentify.dto.TeacherDashboardResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherDashboardController {

    private final CourseService courseService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TeacherDashboardResponse>> getDashboard(
            @RequestHeader("Authorization") String authorizationHeader) {
        ApiResponse<TeacherDashboardResponse> response = courseService.getTeacherDashboard(authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
