package com.mentify.attendance.service.impl;

import com.mentify.attendance.client.CourseServiceClient;
import com.mentify.attendance.client.EnrollmentServiceClient;
import com.mentify.attendance.client.dto.CourseLookupResponse;
import com.mentify.attendance.dto.request.AttendanceRecordMarkRequest;
import com.mentify.attendance.dto.request.CreateAttendanceSessionRequest;
import com.mentify.attendance.dto.request.MarkAttendanceRequest;
import com.mentify.attendance.dto.response.AttendanceRecordResponse;
import com.mentify.attendance.dto.response.AttendanceSessionResponse;
import com.mentify.attendance.entity.AttendanceRecord;
import com.mentify.attendance.entity.AttendanceSession;
import com.mentify.attendance.enums.AttendanceMode;
import com.mentify.attendance.enums.AttendanceSessionStatus;
import com.mentify.attendance.enums.AttendanceStatus;
import com.mentify.attendance.repository.AttendanceRecordRepository;
import com.mentify.attendance.repository.AttendanceSessionRepository;
import com.mentify.attendance.security.CurrentUserService;
import com.mentify.attendance.service.AttendanceSessionService;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceSessionServiceImpl implements AttendanceSessionService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final CourseServiceClient courseServiceClient;
    private final EnrollmentServiceClient enrollmentServiceClient;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ApiResponse<AttendanceSessionResponse> createSession(CreateAttendanceSessionRequest request, String authorizationHeader) {
        CourseLookupResponse course = getCourseOrThrow(request.getCourseId(), authorizationHeader);
        assertTeacherCanManageCourse(course);
        validateSessionTimes(request.getStartTime(), request.getEndTime());

        if (attendanceSessionRepository.existsByCourseIdAndAttendanceDateAndStartTimeAndIsActiveTrue(
                request.getCourseId(), request.getAttendanceDate(), request.getStartTime())) {
            throw new ResourceAlreadyExistsException(
                    "An attendance session already exists for this course on the given date and start time");
        }

        AttendanceSession session = AttendanceSession.builder()
                .courseId(request.getCourseId())
                .title(request.getTitle().trim())
                .attendanceDate(request.getAttendanceDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .mode(AttendanceMode.PHYSICAL)
                .status(AttendanceSessionStatus.OPEN)
                .build();
        session = attendanceSessionRepository.save(session);

        List<UUID> enrolledStudentIds = getEnrolledStudentIds(request.getCourseId(), authorizationHeader);
        List<AttendanceRecord> records = new ArrayList<>();
        for (UUID studentId : enrolledStudentIds) {
            records.add(AttendanceRecord.builder()
                    .session(session)
                    .studentId(studentId)
                    .status(AttendanceStatus.NOT_MARKED)
                    .build());
        }
        records = attendanceRecordRepository.saveAll(records);

        return response(HttpStatus.CREATED, "Attendance session created successfully", mapToResponse(session, records));
    }

    @Override
    public ApiResponse<List<AttendanceSessionResponse>> listSessionsForCourse(UUID courseId, String authorizationHeader) {
        CourseLookupResponse course = getCourseOrThrow(courseId, authorizationHeader);
        assertTeacherCanManageCourse(course);

        List<AttendanceSessionResponse> sessions = attendanceSessionRepository
                .findByCourseIdAndIsActiveTrueOrderByAttendanceDateDescStartTimeDesc(courseId)
                .stream()
                .map(session -> mapToResponse(session, null))
                .toList();

        return response(HttpStatus.OK, "Attendance sessions fetched successfully", sessions);
    }

    @Override
    public ApiResponse<AttendanceSessionResponse> getSession(UUID sessionId, String authorizationHeader) {
        AttendanceSession session = getSessionOrThrow(sessionId);
        CourseLookupResponse course = getCourseOrThrow(session.getCourseId(), authorizationHeader);
        assertTeacherCanManageCourse(course);

        List<AttendanceRecord> records = attendanceRecordRepository.findBySession_IdAndIsActiveTrue(sessionId);
        return response(HttpStatus.OK, "Attendance session fetched successfully", mapToResponse(session, records));
    }

    @Override
    @Transactional
    public ApiResponse<AttendanceSessionResponse> markAttendance(UUID sessionId, MarkAttendanceRequest request, String authorizationHeader) {
        AttendanceSession session = getSessionOrThrow(sessionId);
        CourseLookupResponse course = getCourseOrThrow(session.getCourseId(), authorizationHeader);
        assertTeacherCanManageCourse(course);

        if (session.getStatus() != AttendanceSessionStatus.OPEN) {
            throw new ResourceAlreadyExistsException("Attendance can only be marked while the session is OPEN");
        }

        Set<UUID> seenStudentIds = new HashSet<>();
        UUID markedBy = currentUserService.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        for (AttendanceRecordMarkRequest recordRequest : request.getRecords()) {
            if (!seenStudentIds.add(recordRequest.getStudentId())) {
                throw new IllegalArgumentException("Duplicate student entry found in the attendance batch");
            }
            if (recordRequest.getStatus() == AttendanceStatus.NOT_MARKED) {
                throw new IllegalArgumentException("Allowed attendance statuses are PRESENT, ABSENT, and LATE");
            }

            AttendanceRecord record = attendanceRecordRepository
                    .findBySession_IdAndStudentIdAndIsActiveTrue(sessionId, recordRequest.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Student " + recordRequest.getStudentId() + " does not belong to this attendance session"));

            record.setStatus(recordRequest.getStatus());
            record.setRemarks(recordRequest.getRemarks());
            record.setMarkedBy(markedBy);
            record.setMarkedAt(now);
            attendanceRecordRepository.save(record);
        }

        List<AttendanceRecord> records = attendanceRecordRepository.findBySession_IdAndIsActiveTrue(sessionId);
        return response(HttpStatus.OK, "Attendance marked successfully", mapToResponse(session, records));
    }

    @Override
    @Transactional
    public ApiResponse<AttendanceSessionResponse> completeSession(UUID sessionId, String authorizationHeader) {
        AttendanceSession session = getSessionOrThrow(sessionId);
        CourseLookupResponse course = getCourseOrThrow(session.getCourseId(), authorizationHeader);
        assertTeacherCanManageCourse(course);

        if (session.getStatus() != AttendanceSessionStatus.OPEN) {
            throw new ResourceAlreadyExistsException("Only OPEN sessions can be completed");
        }
        if (attendanceRecordRepository.existsBySession_IdAndStatusAndIsActiveTrue(sessionId, AttendanceStatus.NOT_MARKED)) {
            throw new ResourceAlreadyExistsException("Cannot complete session while students remain NOT_MARKED");
        }

        session.setStatus(AttendanceSessionStatus.COMPLETED);
        session = attendanceSessionRepository.save(session);

        List<AttendanceRecord> records = attendanceRecordRepository.findBySession_IdAndIsActiveTrue(sessionId);
        return response(HttpStatus.OK, "Attendance session completed successfully", mapToResponse(session, records));
    }

    @Override
    @Transactional
    public ApiResponse<AttendanceSessionResponse> cancelSession(UUID sessionId, String authorizationHeader) {
        AttendanceSession session = getSessionOrThrow(sessionId);
        CourseLookupResponse course = getCourseOrThrow(session.getCourseId(), authorizationHeader);
        assertTeacherCanManageCourse(course);

        if (session.getStatus() != AttendanceSessionStatus.OPEN) {
            throw new ResourceAlreadyExistsException("Only OPEN sessions can be cancelled");
        }

        session.setStatus(AttendanceSessionStatus.CANCELLED);
        session = attendanceSessionRepository.save(session);

        List<AttendanceRecord> records = attendanceRecordRepository.findBySession_IdAndIsActiveTrue(sessionId);
        return response(HttpStatus.OK, "Attendance session cancelled successfully", mapToResponse(session, records));
    }

    private AttendanceSession getSessionOrThrow(UUID sessionId) {
        return attendanceSessionRepository.findByIdAndIsActiveTrue(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance session not found with id: " + sessionId));
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

    private List<UUID> getEnrolledStudentIds(UUID courseId, String authorizationHeader) {
        ApiResponse<List<UUID>> response = enrollmentServiceClient.getEnrolledStudentIdsByCourse(courseId, authorizationHeader);
        if (response == null || response.getData() == null) {
            return List.of();
        }
        return response.getData();
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

    private void validateSessionTimes(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Session start time must be before end time");
        }
    }

    private AttendanceSessionResponse mapToResponse(AttendanceSession session, List<AttendanceRecord> records) {
        return AttendanceSessionResponse.builder()
                .id(session.getId())
                .courseId(session.getCourseId())
                .title(session.getTitle())
                .attendanceDate(session.getAttendanceDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .mode(session.getMode())
                .status(session.getStatus())
                .createdBy(session.getCreatedBy())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .records(records == null ? null : records.stream().map(this::mapRecord).toList())
                .build();
    }

    private AttendanceRecordResponse mapRecord(AttendanceRecord record) {
        return AttendanceRecordResponse.builder()
                .id(record.getId())
                .studentId(record.getStudentId())
                .status(record.getStatus())
                .remarks(record.getRemarks())
                .markedBy(record.getMarkedBy())
                .markedAt(record.getMarkedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .statusCode(status.value())
                .message(message)
                .data(data)
                .build();
    }
}
