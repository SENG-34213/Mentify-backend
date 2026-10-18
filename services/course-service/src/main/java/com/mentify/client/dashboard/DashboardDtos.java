package com.mentify.client.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public final class DashboardDtos {
    private DashboardDtos() {
    }

    public record Assignment(UUID id, UUID courseId, String title, LocalDateTime dueDate,
                             String status, Long totalSubmissions, Long gradedCount) {
    }

    public record Submission(UUID id, UUID assignmentId, UUID studentId, String studentName,
                             String status, LocalDateTime submittedAt) {
    }

    public record Exam(UUID id, UUID courseId, String title, LocalDate examDate,
                       LocalTime startTime, LocalTime endTime, String status) {
    }

    public record CalendarEvent(UUID id, String title, String eventType,
                                LocalDateTime startDateTime, LocalDateTime endDateTime,
                                UUID courseId, String location) {
    }

    public record PendingGrading(UUID submissionId, UUID assignmentId, UUID courseId,
                                 String assignmentTitle, UUID studentId, String studentName,
                                 LocalDateTime submittedAt) {
    }

    public record CoursePerformance(UUID courseId, String courseName, long studentCount,
                                    long activeAssignmentCount, long submissionCount,
                                    long gradedSubmissionCount, double gradingRate) {
    }
}
