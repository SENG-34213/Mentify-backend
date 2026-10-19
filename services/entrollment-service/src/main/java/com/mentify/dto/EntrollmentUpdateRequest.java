package com.mentify.dto;

import com.mentify.enums.EnrollmentStatus;
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

    private List<UUID> courseIds;

    private EnrollmentStatus status;
}

