package com.mentify.attendance.service.impl;

import com.mentify.attendance.client.CourseServiceClient;
import com.mentify.attendance.client.EnrollmentServiceClient;
import com.mentify.attendance.client.dto.CourseLookupResponse;
import com.mentify.attendance.dto.request.AttendanceRecordMarkRequest;
import com.mentify.attendance.dto.request.CreateAttendanceSessionRequest;
import com.mentify.attendance.dto.request.MarkAttendanceRequest;
import com.mentify.attendance.dto.response.AttendanceSessionResponse;
import com.mentify.attendance.entity.AttendanceRecord;
import com.mentify.attendance.entity.AttendanceSession;
import com.mentify.attendance.enums.AttendanceSessionStatus;
import com.mentify.attendance.enums.AttendanceStatus;
import com.mentify.attendance.repository.AttendanceRecordRepository;
import com.mentify.attendance.repository.AttendanceSessionRepository;
import com.mentify.attendance.security.CurrentUserService;
import com.mentify.attendance.service.AttendanceSessionService;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceSessionServiceImplTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private AttendanceSessionRepository attendanceSessionRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private EnrollmentServiceClient enrollmentServiceClient;

    @Mock
    private CurrentUserService currentUserService;

    private AttendanceSessionService attendanceSessionService;

    @BeforeEach
    void setUp() {
        attendanceSessionService = new AttendanceSessionServiceImpl(
                attendanceSessionRepository,
                attendanceRecordRepository,
                courseServiceClient,
                enrollmentServiceClient,
                currentUserService
        );
    }

    private CourseLookupResponse course(UUID courseId, UUID teacherId) {
        CourseLookupResponse course = new CourseLookupResponse();
        course.setId(courseId);
        course.setAssignedTeacherId(teacherId);
        return course;
    }

    @Test
    void createSession_whenTeacherOwnsCourseAndNoDuplicate_createsSessionWithNotMarkedRecords() {
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID student1 = UUID.randomUUID();
        UUID student2 = UUID.randomUUID();

        CreateAttendanceSessionRequest request = CreateAttendanceSessionRequest.builder()
                .courseId(courseId)
                .title("Java - Week 04")
                .attendanceDate(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .data(course(courseId, teacherId))
                .build());
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(attendanceSessionRepository.existsByCourseIdAndAttendanceDateAndStartTimeAndIsActiveTrue(
                eq(courseId), any(), any())).thenReturn(false);
        when(attendanceSessionRepository.save(any(AttendanceSession.class))).thenAnswer(invocation -> {
            AttendanceSession session = invocation.getArgument(0);
            session.setId(UUID.randomUUID());
            return session;
        });
        when(enrollmentServiceClient.getEnrolledStudentIdsByCourse(courseId, AUTH_HEADER))
                .thenReturn(ApiResponse.<List<UUID>>builder().data(List.of(student1, student2)).build());
        when(attendanceRecordRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<AttendanceSessionResponse> response = attendanceSessionService.createSession(request, AUTH_HEADER);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getData().getStatus()).isEqualTo(AttendanceSessionStatus.OPEN);
        assertThat(response.getData().getRecords()).hasSize(2);
        assertThat(response.getData().getRecords())
                .allMatch(record -> record.getStatus() == AttendanceStatus.NOT_MARKED);
    }

    @Test
    void createSession_whenTeacherDoesNotOwnCourse_throwsAccessDenied() {
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID otherTeacherId = UUID.randomUUID();

        CreateAttendanceSessionRequest request = CreateAttendanceSessionRequest.builder()
                .courseId(courseId)
                .title("Java - Week 04")
                .attendanceDate(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .data(course(courseId, otherTeacherId))
                .build());
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);

        assertThrows(AccessDeniedException.class, () -> attendanceSessionService.createSession(request, AUTH_HEADER));
    }

    @Test
    void createSession_whenDuplicateSessionExists_throwsResourceAlreadyExists() {
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        CreateAttendanceSessionRequest request = CreateAttendanceSessionRequest.builder()
                .courseId(courseId)
                .title("Java - Week 04")
                .attendanceDate(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .data(course(courseId, teacherId))
                .build());
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(true);
        when(attendanceSessionRepository.existsByCourseIdAndAttendanceDateAndStartTimeAndIsActiveTrue(
                eq(courseId), any(), any())).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class,
                () -> attendanceSessionService.createSession(request, AUTH_HEADER));
    }

    @Test
    void createSession_whenStartTimeAfterEndTime_throwsIllegalArgument() {
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        CreateAttendanceSessionRequest request = CreateAttendanceSessionRequest.builder()
                .courseId(courseId)
                .title("Java - Week 04")
                .attendanceDate(LocalDate.now())
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .data(course(courseId, teacherId))
                .build());
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> attendanceSessionService.createSession(request, AUTH_HEADER));
    }

    @Test
    void markAttendance_whenSessionOpenAndValidBatch_updatesRecords() {
        UUID sessionId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        AttendanceSession session = AttendanceSession.builder()
                .courseId(courseId)
                .status(AttendanceSessionStatus.OPEN)
                .build();
        session.setId(sessionId);

        AttendanceRecord record = AttendanceRecord.builder()
                .session(session)
                .studentId(studentId)
                .status(AttendanceStatus.NOT_MARKED)
                .build();

        MarkAttendanceRequest request = MarkAttendanceRequest.builder()
                .records(List.of(AttendanceRecordMarkRequest.builder()
                        .studentId(studentId)
                        .status(AttendanceStatus.PRESENT)
                        .build()))
                .build();

        when(attendanceSessionRepository.findByIdAndIsActiveTrue(sessionId)).thenReturn(Optional.of(session));
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .data(course(courseId, teacherId))
                .build());
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUserService.hasAnyRole("TEACHER")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(attendanceRecordRepository.findBySession_IdAndStudentIdAndIsActiveTrue(sessionId, studentId))
                .thenReturn(Optional.of(record));
        when(attendanceRecordRepository.findBySession_IdAndIsActiveTrue(sessionId)).thenReturn(List.of(record));

        ApiResponse<AttendanceSessionResponse> response = attendanceSessionService.markAttendance(sessionId, request, AUTH_HEADER);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(record.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(record.getMarkedBy()).isEqualTo(teacherId);
    }

    @Test
    void markAttendance_whenSessionNotOpen_throwsConflict() {
        UUID sessionId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        AttendanceSession session = AttendanceSession.builder()
                .courseId(courseId)
                .status(AttendanceSessionStatus.COMPLETED)
                .build();
        session.setId(sessionId);

        MarkAttendanceRequest request = MarkAttendanceRequest.builder()
                .records(List.of(AttendanceRecordMarkRequest.builder()
                        .studentId(UUID.randomUUID())
                        .status(AttendanceStatus.PRESENT)
                        .build()))
                .build();

        when(attendanceSessionRepository.findByIdAndIsActiveTrue(sessionId)).thenReturn(Optional.of(session));
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .data(course(courseId, teacherId))
                .build());
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class,
                () -> attendanceSessionService.markAttendance(sessionId, request, AUTH_HEADER));
    }

    @Test
    void markAttendance_whenStudentNotPartOfSession_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID unknownStudent = UUID.randomUUID();

        AttendanceSession session = AttendanceSession.builder()
                .courseId(courseId)
                .status(AttendanceSessionStatus.OPEN)
                .build();
        session.setId(sessionId);

        MarkAttendanceRequest request = MarkAttendanceRequest.builder()
                .records(List.of(AttendanceRecordMarkRequest.builder()
                        .studentId(unknownStudent)
                        .status(AttendanceStatus.PRESENT)
                        .build()))
                .build();

        when(attendanceSessionRepository.findByIdAndIsActiveTrue(sessionId)).thenReturn(Optional.of(session));
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .data(course(courseId, teacherId))
                .build());
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(teacherId);
        when(attendanceRecordRepository.findBySession_IdAndStudentIdAndIsActiveTrue(sessionId, unknownStudent))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> attendanceSessionService.markAttendance(sessionId, request, AUTH_HEADER));
    }

    @Test
    void completeSession_whenNotMarkedRecordsRemain_throwsConflict() {
        UUID sessionId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        AttendanceSession session = AttendanceSession.builder()
                .courseId(courseId)
                .status(AttendanceSessionStatus.OPEN)
                .build();
        session.setId(sessionId);

        when(attendanceSessionRepository.findByIdAndIsActiveTrue(sessionId)).thenReturn(Optional.of(session));
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .data(course(courseId, teacherId))
                .build());
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(true);
        when(attendanceRecordRepository.existsBySession_IdAndStatusAndIsActiveTrue(sessionId, AttendanceStatus.NOT_MARKED))
                .thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class,
                () -> attendanceSessionService.completeSession(sessionId, AUTH_HEADER));
    }

    @Test
    void completeSession_whenAllMarked_completesSuccessfully() {
        UUID sessionId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        AttendanceSession session = AttendanceSession.builder()
                .courseId(courseId)
                .status(AttendanceSessionStatus.OPEN)
                .build();
        session.setId(sessionId);

        when(attendanceSessionRepository.findByIdAndIsActiveTrue(sessionId)).thenReturn(Optional.of(session));
        when(courseServiceClient.getCourseById(courseId, AUTH_HEADER)).thenReturn(ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .data(course(courseId, teacherId))
                .build());
        when(currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(true);
        when(attendanceRecordRepository.existsBySession_IdAndStatusAndIsActiveTrue(sessionId, AttendanceStatus.NOT_MARKED))
                .thenReturn(false);
        when(attendanceSessionRepository.save(any(AttendanceSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(attendanceRecordRepository.findBySession_IdAndIsActiveTrue(sessionId)).thenReturn(List.of());

        ApiResponse<AttendanceSessionResponse> response = attendanceSessionService.completeSession(sessionId, AUTH_HEADER);

        assertThat(response.getData().getStatus()).isEqualTo(AttendanceSessionStatus.COMPLETED);
    }

    @Test
    void cancelSession_whenSessionNotFound_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(attendanceSessionRepository.findByIdAndIsActiveTrue(sessionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> attendanceSessionService.cancelSession(sessionId, AUTH_HEADER));
    }
}
