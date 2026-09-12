package com.mentify.attendance.service.impl;

import com.mentify.attendance.client.CourseServiceClient;
import com.mentify.attendance.client.dto.CourseLookupResponse;
import com.mentify.attendance.dto.response.AttendanceSummaryResponse;
import com.mentify.attendance.entity.AttendanceRecord;
import com.mentify.attendance.enums.AttendanceSessionStatus;
import com.mentify.attendance.enums.AttendanceStatus;
import com.mentify.attendance.repository.AttendanceRecordRepository;
import com.mentify.attendance.security.CurrentUserService;
import com.mentify.attendance.service.AttendanceSummaryService;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceSummaryServiceImpl implements AttendanceSummaryService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final CourseServiceClient courseServiceClient;
    private final CurrentUserService currentUserService;

    @Override
    public ApiResponse<AttendanceSummaryResponse> getMyAttendanceSummary(UUID courseId, String authorizationHeader) {
        // course-service's /lookup endpoint is teacher/admin-only, so students can't use it here;
        // scoping records to the student's own JWT id is sufficient authorization for /me.
        UUID studentId = currentUserService.getCurrentUserId();
        AttendanceSummaryResponse summary = buildSummary(studentId, courseId);
        return response(HttpStatus.OK, "Attendance summary fetched successfully", summary);
    }

    @Override
    public ApiResponse<AttendanceSummaryResponse> getStudentAttendanceSummary(UUID studentId, UUID courseId, String authorizationHeader) {
        CourseLookupResponse course = getCourseOrThrow(courseId, authorizationHeader);
        assertTeacherCanManageCourse(course);
        AttendanceSummaryResponse summary = buildSummary(studentId, courseId);
        return response(HttpStatus.OK, "Student attendance summary fetched successfully", summary);
    }

    private AttendanceSummaryResponse buildSummary(UUID studentId, UUID courseId) {
        List<AttendanceRecord> records = attendanceRecordRepository
                .findByStudentIdAndSession_CourseIdAndSession_StatusAndIsActiveTrue(
                        studentId, courseId, AttendanceSessionStatus.COMPLETED);

        long completedSessions = records.size();
        long presentCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        long lateCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
        long absentCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
        long attendedCount = presentCount + lateCount;
        double percentage = completedSessions == 0
                ? 0.0
                : Math.round((attendedCount * 10000.0) / completedSessions) / 100.0;

        return AttendanceSummaryResponse.builder()
                .studentId(studentId)
                .courseId(courseId)
                .completedSessions(completedSessions)
                .presentCount(presentCount)
                .lateCount(lateCount)
                .absentCount(absentCount)
                .attendedCount(attendedCount)
                .attendancePercentage(percentage)
                .build();
    }

    private CourseLookupResponse getCourseOrThrow(UUID courseId, String authorizationHeader) {
        try {
            ApiResponse<CourseLookupResponse> response = courseServiceClient.getCourseById(courseId, authorizationHeader);
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException("Course not found with id: " + courseId);
            }
            return response.getData();
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
    }

    private void assertTeacherCanManageCourse(CourseLookupResponse course) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        if (!currentUserService.hasAnyRole("TEACHER")) {
            throw new AccessDeniedException("Access denied");
        }
        UUID currentUserId = currentUserService.getCurrentUserId();
        if (!currentUserId.equals(course.getAssignedTeacherId())) {
            throw new AccessDeniedException("Teacher can only manage assigned courses");
        }
    }

    private ApiResponse<AttendanceSummaryResponse> response(HttpStatus status, String message, AttendanceSummaryResponse data) {
        return ApiResponse.<AttendanceSummaryResponse>builder()
                .status(status)
                .statusCode(status.value())
                .message(message)
                .data(data)
                .build();
    }
}
