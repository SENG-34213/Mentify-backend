package com.mentify.attendance.service.impl;

import com.mentify.attendance.client.CourseServiceClient;
import com.mentify.attendance.client.dto.CourseLookupResponse;
import com.mentify.attendance.dto.response.AttendanceSummaryResponse;
import com.mentify.attendance.entity.AttendanceRecord;
import com.mentify.attendance.entity.AttendanceSession;
import com.mentify.attendance.enums.AttendanceSessionStatus;
import com.mentify.attendance.enums.AttendanceStatus;
import com.mentify.attendance.repository.AttendanceRecordRepository;
import com.mentify.attendance.security.CurrentUserService;
import com.mentify.attendance.service.AttendanceSummaryService;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceSummaryServiceImplTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private CurrentUserService currentUserService;

    private AttendanceSummaryService attendanceSummaryService;

    @BeforeEach
    void setUp() {
        attendanceSummaryService = new AttendanceSummaryServiceImpl(
                attendanceRecordRepository, courseServiceClient, currentUserService);
    }

    private AttendanceRecord recordWithStatus(AttendanceStatus status) {
        AttendanceSession session = AttendanceSession.builder().status(AttendanceSessionStatus.COMPLETED).build();
        return AttendanceRecord.builder().session(session).status(status).build();
    }

    @Test
    void getMyAttendanceSummary_computesPercentageFromPresentAndLate() {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(attendanceRecordRepository.findByStudentIdAndSession_CourseIdAndSession_StatusAndIsActiveTrue(
                studentId, courseId, AttendanceSessionStatus.COMPLETED))
                .thenReturn(List.of(
                        recordWithStatus(AttendanceStatus.PRESENT),
                        recordWithStatus(AttendanceStatus.PRESENT),
                        recordWithStatus(AttendanceStatus.LATE),
                        recordWithStatus(AttendanceStatus.ABSENT)
                ));

        ApiResponse<AttendanceSummaryResponse> response = attendanceSummaryService.getMyAttendanceSummary(courseId, AUTH_HEADER);

        AttendanceSummaryResponse summary = response.getData();
        assertThat(summary.getCompletedSessions()).isEqualTo(4);
        assertThat(summary.getPresentCount()).isEqualTo(2);
        assertThat(summary.getLateCount()).isEqualTo(1);
        assertThat(summary.getAbsentCount()).isEqualTo(1);
        assertThat(summary.getAttendedCount()).isEqualTo(3);
        assertThat(summary.getAttendancePercentage()).isEqualTo(75.0);
    }

    @Test
    void getMyAttendanceSummary_whenNoCompletedSessions_returnsZeroPercentage() {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(attendanceRecordRepository.findByStudentIdAndSession_CourseIdAndSession_StatusAndIsActiveTrue(
                studentId, courseId, AttendanceSessionStatus.COMPLETED))
                .thenReturn(List.of());

        ApiResponse<AttendanceSummaryResponse> response = attendanceSummaryService.getMyAttendanceSummary(courseId, AUTH_HEADER);

        assertThat(response.getData().getAttendancePercentage()).isEqualTo(0.0);
    }

    @Test
    void getStudentAttendanceSummary_whenTeacherNotAssignedToCourse_throwsAccessDenied() {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID otherTeacherId = UUID.randomUUID();

        CourseLookupResponse course = new CourseLookupResponse();
        course.setId(courseId);
        course.setAssignedTeacherId(otherTeacherId);

        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .data(course)
                .build());
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);

        assertThrows(AccessDeniedException.class,
                () -> attendanceSummaryService.getStudentAttendanceSummary(studentId, courseId, AUTH_HEADER));
    }
}
