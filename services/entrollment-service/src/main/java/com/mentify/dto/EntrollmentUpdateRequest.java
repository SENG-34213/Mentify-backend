package com.mentify.dto;

import jakarta.validation.constraints.NotEmpty;
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
public class EntrollmentUpdateRequest {

    @NotEmpty(message = "At least one course ID is required")
    private List<UUID> courseIds;
}

