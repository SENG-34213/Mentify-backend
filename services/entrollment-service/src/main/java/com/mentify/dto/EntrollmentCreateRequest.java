package com.mentify.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntrollmentCreateRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotEmpty(message = "At least one course ID is required")
    private List<UUID> courseIds;
}

