package com.mentify.attendance.controller;

import com.mentify.attendance.dto.response.AttendanceSummaryResponse;
import com.mentify.attendance.service.AttendanceSummaryService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/attendance")
public class AttendanceSummaryController {

    private final AttendanceSummaryService attendanceSummaryService;

    @GetMapping("/courses/{courseId}/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getMyAttendanceSummary(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AttendanceSummaryResponse> response =
                attendanceSummaryService.getMyAttendanceSummary(courseId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/students/{studentId}/courses/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getStudentAttendanceSummary(
            @PathVariable UUID studentId,
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AttendanceSummaryResponse> response =
                attendanceSummaryService.getStudentAttendanceSummary(studentId, courseId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
