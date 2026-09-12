package com.mentify.attendance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAttendanceRequest {

    @NotEmpty(message = "At least one attendance record is required")
    @Valid
    private List<AttendanceRecordMarkRequest> records;
}
