package com.mentify.attendance.dto.request;

import com.mentify.attendance.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordMarkRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Attendance status is required")
    private AttendanceStatus status;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}
