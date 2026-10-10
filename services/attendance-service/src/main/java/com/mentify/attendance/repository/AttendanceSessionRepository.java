package com.mentify.attendance.repository;

import com.mentify.attendance.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {

    Optional<AttendanceSession> findByIdAndIsActiveTrue(UUID id);

    List<AttendanceSession> findByCourseIdAndIsActiveTrueOrderByAttendanceDateDescStartTimeDesc(UUID courseId);

    boolean existsByCourseIdAndAttendanceDateAndStartTimeAndIsActiveTrue(
            UUID courseId, LocalDate attendanceDate, LocalTime startTime);
}
