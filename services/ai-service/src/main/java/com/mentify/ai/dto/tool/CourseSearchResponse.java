package com.mentify.ai.dto.tool;

import java.util.List;
import java.util.UUID;

public record CourseSearchResponse(List<CourseSearchResult> results, int total) {

    public record CourseSearchResult(
            UUID id,
            String type,
            String title,
            String subtitle,
            UUID courseId,
            String navigationTarget
    ) {
    }
}
