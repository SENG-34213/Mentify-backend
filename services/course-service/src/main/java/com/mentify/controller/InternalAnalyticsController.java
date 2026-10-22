package com.mentify.controller;

import com.mentify.dto.CourseAnalyticsOverview;
import com.mentify.enums.CourseStatus;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/internal/analytics/courses")
@RequiredArgsConstructor
public class InternalAnalyticsController {

    private final CourseRepository courseRepository;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CourseAnalyticsOverview>> getOverview() {
        LocalDateTime monthStart = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        CourseAnalyticsOverview overview = new CourseAnalyticsOverview(
                courseRepository.count(),
                courseRepository.countByStatusNot(CourseStatus.ARCHIVED),
                courseRepository.countByCreatedAtAfter(monthStart)
        );

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Course analytics overview fetched successfully", overview));
    }
}
