package com.mentify.material.client.dto;

import java.util.UUID;

public record CourseLookupResponse(
        UUID id,
        String courseName,
        String courseCode,
        String level,
        String semester,
        String academicYear,
        Integer maxCapacity,
        UUID assignedTeacherId
) {
    public CourseLookupResponse(UUID id, UUID assignedTeacherId) {
        this(id, null, null, null, null, null, null, assignedTeacherId);
    }
}
