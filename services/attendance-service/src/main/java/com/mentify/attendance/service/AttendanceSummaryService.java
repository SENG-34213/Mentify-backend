package com.mentify.attendance.service;

import com.mentify.attendance.dto.response.AttendanceSummaryResponse;
import com.mentify.attendance.dto.response.AttendanceAnalyticsSummaryResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;
import java.time.LocalDate;

public interface AttendanceSummaryService {

    ApiResponse<AttendanceSummaryResponse> getMyAttendanceSummary(UUID courseId, String authorizationHeader);

    ApiResponse<AttendanceSummaryResponse> getStudentAttendanceSummary(UUID studentId, UUID courseId, String authorizationHeader);

    ApiResponse<AttendanceAnalyticsSummaryResponse> getAnalyticsOverview();

    ApiResponse<AttendanceAnalyticsSummaryResponse> getAnalyticsCourseSummary(UUID courseId);

    ApiResponse<AttendanceAnalyticsSummaryResponse> getAnalyticsOverview(LocalDate from, LocalDate to);

    ApiResponse<AttendanceAnalyticsSummaryResponse> getAnalyticsCourseSummary(
            UUID courseId, LocalDate from, LocalDate to, String authorizationHeader);
}
