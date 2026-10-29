package com.mentify.attendance.controller;

import com.mentify.attendance.dto.request.CreateAttendanceSessionRequest;
import com.mentify.attendance.dto.request.MarkAttendanceRequest;
import com.mentify.attendance.dto.response.AttendanceSessionResponse;
import com.mentify.attendance.service.AttendanceSessionService;
import com.mentify.common.notification.NotifyOnSuccess;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/attendance")
public class AttendanceSessionController {

    private final AttendanceSessionService attendanceSessionService;

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @NotifyOnSuccess(type = "ATTENDANCE_SESSION_CREATED", title = "Attendance session created", message = "Your attendance session was created successfully.")
    public ResponseEntity<ApiResponse<AttendanceSessionResponse>> createSession(
            @Valid @RequestBody CreateAttendanceSessionRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AttendanceSessionResponse> response = attendanceSessionService.createSession(request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/courses/{courseId}/sessions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceSessionResponse>>> listSessionsForCourse(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<List<AttendanceSessionResponse>> response =
                attendanceSessionService.listSessionsForCourse(courseId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceSessionResponse>> getSession(
            @PathVariable UUID sessionId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AttendanceSessionResponse> response = attendanceSessionService.getSession(sessionId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/sessions/{sessionId}/records")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @NotifyOnSuccess(type = "ATTENDANCE_RECORDED", title = "Attendance recorded", message = "Attendance records were saved successfully.")
    public ResponseEntity<ApiResponse<AttendanceSessionResponse>> markAttendance(
            @PathVariable UUID sessionId,
            @Valid @RequestBody MarkAttendanceRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AttendanceSessionResponse> response =
                attendanceSessionService.markAttendance(sessionId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/sessions/{sessionId}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @NotifyOnSuccess(type = "ATTENDANCE_SESSION_COMPLETED", title = "Attendance session completed", message = "Your attendance session was completed successfully.")
    public ResponseEntity<ApiResponse<AttendanceSessionResponse>> completeSession(
            @PathVariable UUID sessionId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AttendanceSessionResponse> response = attendanceSessionService.completeSession(sessionId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @NotifyOnSuccess(type = "ATTENDANCE_SESSION_CANCELLED", title = "Attendance session cancelled", message = "Your attendance session was cancelled successfully.")
    public ResponseEntity<ApiResponse<AttendanceSessionResponse>> cancelSession(
            @PathVariable UUID sessionId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AttendanceSessionResponse> response = attendanceSessionService.cancelSession(sessionId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
    @NotifyOnSuccess(type = "ATTENDANCE_SESSION_DELETED", title = "Attendance session deleted", message = "Your attendance session was deleted successfully.")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable UUID sessionId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<Void> response = attendanceSessionService.deleteSession(sessionId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
