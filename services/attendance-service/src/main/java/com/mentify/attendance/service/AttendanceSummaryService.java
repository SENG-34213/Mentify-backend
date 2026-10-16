package com.mentify.attendance.service;

import com.mentify.attendance.dto.response.AttendanceSummaryResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface AttendanceSummaryService {

    ApiResponse<AttendanceSummaryResponse> getMyAttendanceSummary(UUID courseId, String authorizationHeader);

    ApiResponse<AttendanceSummaryResponse> getStudentAttendanceSummary(UUID studentId, UUID courseId, String authorizationHeader);
}
