package com.mentify.service;

import com.mentify.dto.TeacherAnalyticsResponse;
import com.mentify.enums.TeacherAnalyticsPeriod;
import com.mentify.payload.response.ApiResponse;

import java.time.LocalDate;

public interface TeacherAnalyticsService {
    ApiResponse<TeacherAnalyticsResponse> getAnalytics(
            java.util.UUID courseId,
            TeacherAnalyticsPeriod period,
            LocalDate from,
            LocalDate to,
            String authorizationHeader);
}
