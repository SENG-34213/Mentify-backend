package com.mentify.attendance.service;

import com.mentify.attendance.dto.request.CreateAttendanceSessionRequest;
import com.mentify.attendance.dto.request.MarkAttendanceRequest;
import com.mentify.attendance.dto.response.AttendanceSessionResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface AttendanceSessionService {

    ApiResponse<AttendanceSessionResponse> createSession(CreateAttendanceSessionRequest request, String authorizationHeader);

    ApiResponse<List<AttendanceSessionResponse>> listSessionsForCourse(UUID courseId, String authorizationHeader);

    ApiResponse<AttendanceSessionResponse> getSession(UUID sessionId, String authorizationHeader);

    ApiResponse<AttendanceSessionResponse> markAttendance(UUID sessionId, MarkAttendanceRequest request, String authorizationHeader);

    ApiResponse<AttendanceSessionResponse> completeSession(UUID sessionId, String authorizationHeader);

    ApiResponse<AttendanceSessionResponse> cancelSession(UUID sessionId, String authorizationHeader);
}
