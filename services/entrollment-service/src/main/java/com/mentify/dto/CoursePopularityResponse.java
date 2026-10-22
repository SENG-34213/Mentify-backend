package com.mentify.dto;

import java.util.UUID;

public record CoursePopularityResponse(UUID courseId, long enrollmentCount) {
}
