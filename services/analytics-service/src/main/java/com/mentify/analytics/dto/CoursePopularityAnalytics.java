package com.mentify.analytics.dto;

import java.util.UUID;

public record CoursePopularityAnalytics(UUID courseId, long enrollmentCount) {
}
