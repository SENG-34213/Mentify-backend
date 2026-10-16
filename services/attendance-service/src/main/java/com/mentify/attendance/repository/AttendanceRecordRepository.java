package com.mentify.attendance.repository;

import com.mentify.attendance.entity.AttendanceRecord;
import com.mentify.attendance.enums.AttendanceSessionStatus;
import com.mentify.attendance.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    List<AttendanceRecord> findBySession_IdAndIsActiveTrue(UUID sessionId);

    Optional<AttendanceRecord> findBySession_IdAndStudentIdAndIsActiveTrue(UUID sessionId, UUID studentId);

    boolean existsBySession_IdAndStatusAndIsActiveTrue(UUID sessionId, AttendanceStatus status);

    List<AttendanceRecord> findByStudentIdAndSession_CourseIdAndSession_StatusAndIsActiveTrue(
            UUID studentId, UUID courseId, AttendanceSessionStatus sessionStatus);
}
