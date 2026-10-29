package com.mentify.ai.dto.tool;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentAssignmentSummary(
        UUID id,
        UUID courseId,
        String title,
        String assignmentStatus,
        String submissionStatus,
        Integer maxMarks,
        BigDecimal marks) {
}
