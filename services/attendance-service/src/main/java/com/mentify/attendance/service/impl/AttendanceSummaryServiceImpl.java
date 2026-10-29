package com.mentify.attendance.service.impl;

import com.mentify.attendance.client.CourseServiceClient;
import com.mentify.attendance.client.dto.CourseLookupResponse;
import com.mentify.attendance.dto.response.AttendanceSummaryResponse;
import com.mentify.attendance.dto.response.AttendanceAnalyticsSummaryResponse;
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
import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;

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

    @Override
    public ApiResponse<AttendanceAnalyticsSummaryResponse> getAnalyticsOverview() {
        return analyticsResponse(null, attendanceRecordRepository
                .findBySession_StatusAndIsActiveTrue(AttendanceSessionStatus.COMPLETED));
    }

    @Override
    public ApiResponse<AttendanceAnalyticsSummaryResponse> getAnalyticsCourseSummary(UUID courseId) {
        return analyticsResponse(courseId, attendanceRecordRepository
                .findBySession_CourseIdAndSession_StatusAndIsActiveTrue(
                        courseId, AttendanceSessionStatus.COMPLETED));
    }

    @Override
    public ApiResponse<AttendanceAnalyticsSummaryResponse> getAnalyticsOverview(LocalDate from, LocalDate to) {
        DateRange range = normalizeDateRange(from, to);
        List<AttendanceRecord> records = range.isUnbounded()
                ? attendanceRecordRepository.findBySession_StatusAndIsActiveTrue(AttendanceSessionStatus.COMPLETED)
                : attendanceRecordRepository.findBySession_StatusAndSession_AttendanceDateBetweenAndIsActiveTrue(
                        AttendanceSessionStatus.COMPLETED, range.from(), range.to());
        return analyticsResponse(null, records);
    }

    @Override
    public ApiResponse<AttendanceAnalyticsSummaryResponse> getAnalyticsCourseSummary(
            UUID courseId, LocalDate from, LocalDate to, String authorizationHeader) {
        CourseLookupResponse course = getCourseOrThrow(courseId, authorizationHeader);
        assertTeacherCanManageCourse(course);

        DateRange range = normalizeDateRange(from, to);
        List<AttendanceRecord> records = range.isUnbounded()
                ? attendanceRecordRepository.findBySession_CourseIdAndSession_StatusAndIsActiveTrue(
                        courseId, AttendanceSessionStatus.COMPLETED)
                : attendanceRecordRepository.findBySession_CourseIdAndSession_StatusAndSession_AttendanceDateBetweenAndIsActiveTrue(
                        courseId, AttendanceSessionStatus.COMPLETED, range.from(), range.to());
        return analyticsResponse(courseId, records);
    }

    private DateRange normalizeDateRange(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return new DateRange(null, null);
        }
        LocalDate normalizedFrom = from == null ? to : from;
        LocalDate normalizedTo = to == null ? from : to;
        if (normalizedFrom.isAfter(normalizedTo)) {
            throw new IllegalArgumentException("Attendance date 'from' must not be after 'to'");
        }
        return new DateRange(normalizedFrom, normalizedTo);
    }

    private record DateRange(LocalDate from, LocalDate to) {
        private boolean isUnbounded() {
            return from == null && to == null;
        }
    }

    private ApiResponse<AttendanceAnalyticsSummaryResponse> analyticsResponse(
            UUID courseId, List<AttendanceRecord> records) {
        long attended = records.stream()
                .filter(record -> record.getStatus() == AttendanceStatus.PRESENT
                        || record.getStatus() == AttendanceStatus.LATE)
                .count();
        BigDecimal average = records.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(attended * 100.0 / records.size())
                .setScale(2, RoundingMode.HALF_UP);
        return ApiResponse.success(200, "Attendance analytics fetched successfully",
                new AttendanceAnalyticsSummaryResponse(courseId, average));
    }

    private AttendanceSummaryResponse buildSummary(UUID studentId, UUID courseId) {
        List<AttendanceRecord> records = attendanceRecordRepository
                .findByStudentIdAndSession_CourseIdAndIsActiveTrue(studentId, courseId)
                .stream()
                // A marked attendance is meaningful as soon as it is saved; waiting for the
                // teacher to close the session made PRESENT/ABSENT records appear as zeroes.
                // Cancelled sessions and unmarked roster rows are not attendance attempts.
                .filter(record -> record.getSession().getStatus() != AttendanceSessionStatus.CANCELLED)
                .filter(record -> record.getStatus() != AttendanceStatus.NOT_MARKED)
                .toList();

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
