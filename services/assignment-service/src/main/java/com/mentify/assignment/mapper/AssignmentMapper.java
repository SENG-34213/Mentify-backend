package com.mentify.assignment.mapper;

import com.mentify.assignment.dto.response.AssignmentResponse;
import com.mentify.assignment.dto.response.StudentAssignmentResponse;
import com.mentify.assignment.entity.Assignment;

public final class AssignmentMapper {

    private AssignmentMapper() {
    }

    public static AssignmentResponse toAssignmentResponse(Assignment assignment) {
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .courseId(assignment.getCourseId())
                .moduleId(assignment.getModuleId())
                .lessonId(assignment.getLessonId())
                .teacherId(assignment.getTeacherId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .instructions(assignment.getInstructions())
                .startDate(assignment.getStartDate())
                .dueDate(assignment.getDueDate())
                .maxMarks(assignment.getMaxMarks())
                .allowedAttempts(assignment.getAllowedAttempts())
                .lateSubmissionAllowed(assignment.getLateSubmissionAllowed())
                .latePenaltyPercentage(assignment.getLatePenaltyPercentage())
                .status(assignment.getStatus())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }

    public static StudentAssignmentResponse toStudentAssignmentResponse(Assignment assignment) {
        return StudentAssignmentResponse.builder()
                .id(assignment.getId())
                .courseId(assignment.getCourseId())
                .moduleId(assignment.getModuleId())
                .lessonId(assignment.getLessonId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .instructions(assignment.getInstructions())
                .startDate(assignment.getStartDate())
                .dueDate(assignment.getDueDate())
                .maxMarks(assignment.getMaxMarks())
                .allowedAttempts(assignment.getAllowedAttempts())
                .lateSubmissionAllowed(assignment.getLateSubmissionAllowed())
                .latePenaltyPercentage(assignment.getLatePenaltyPercentage())
                .build();
    }
}
