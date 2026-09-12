package com.mentify.attendance.dto.response;

import com.mentify.attendance.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordResponse {
    private UUID id;
    private UUID studentId;
    private AttendanceStatus status;
    private String remarks;
    private UUID markedBy;
    private LocalDateTime markedAt;
    private LocalDateTime updatedAt;
}
