package com.mentify.controller;

import com.mentify.dto.TeacherAnalyticsResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.TeacherAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherAnalyticsController {
    private final TeacherAnalyticsService analyticsService;

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TeacherAnalyticsResponse>> getAnalytics(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestHeader("Authorization") String authorizationHeader) {
        ApiResponse<TeacherAnalyticsResponse> response = analyticsService
                .getAnalytics(courseId, from, to, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
