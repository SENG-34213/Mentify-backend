package com.mentify.material.client.dto;

import java.util.UUID;

public record CourseLookupResponse(
        UUID id,
        UUID assignedTeacherId
) {
}
