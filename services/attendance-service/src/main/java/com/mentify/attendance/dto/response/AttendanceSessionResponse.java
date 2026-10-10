package com.mentify.attendance.dto.response;

import com.mentify.attendance.enums.AttendanceMode;
import com.mentify.attendance.enums.AttendanceSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSessionResponse {
    private UUID id;
    private UUID courseId;
    private String title;
    private LocalDate attendanceDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private AttendanceMode mode;
    private AttendanceSessionStatus status;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AttendanceRecordResponse> records;
}
