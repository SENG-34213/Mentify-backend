package com.mentify.assignment.service;

import com.mentify.assignment.dto.response.SubmissionResponse;
import com.mentify.assignment.entity.Submission;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.UserServiceClient;
import com.mentify.quiz.client.dto.StudentLookupRequest;
import com.mentify.quiz.client.dto.StudentLookupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class SubmissionResponseMapper {

    private final UserServiceClient userServiceClient;

    public SubmissionResponse toResponse(Submission submission, String authorizationHeader) {
        return toResponses(List.of(submission), authorizationHeader).get(0);
    }

    public List<SubmissionResponse> toResponses(List<Submission> submissions, String authorizationHeader) {
        if (submissions.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> studentNames = new HashMap<>();
        submissions.stream()
                .map(Submission::getStudentId)
                .distinct()
                .forEach(studentId -> studentNames.put(studentId, null));
        List<UUID> studentIds = List.copyOf(studentNames.keySet());
        for (int start = 0; start < studentIds.size(); start += 500) {
            List<UUID> batch = studentIds.subList(start, Math.min(start + 500, studentIds.size()));
            ApiResponse<List<StudentLookupResponse>> lookupResponse =
                    userServiceClient.lookupStudents(new StudentLookupRequest(batch), authorizationHeader);
            if (lookupResponse == null || lookupResponse.getData() == null) {
                throw new IllegalStateException("Student details could not be loaded");
            }
            lookupResponse.getData().forEach(student ->
                    studentNames.put(student.getStudentId(), fullName(student)));
        }
        return submissions.stream()
                .map(submission -> buildResponse(submission, studentNames.get(submission.getStudentId())))
                .toList();
    }

    private SubmissionResponse buildResponse(Submission submission, String studentName) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment() != null ? submission.getAssignment().getId() : null)
                .studentId(submission.getStudentId())
                .studentName(studentName)
                .attemptNumber(submission.getAttemptNumber())
                .status(submission.getStatus())
                .content(submission.getContent())
                .isLate(submission.getIsLate())
                .submittedAt(submission.getSubmittedAt())
                .marks(submission.getMarks())
                .feedback(submission.getFeedback())
                .gradedAt(submission.getGradedAt())
                .gradedBy(submission.getGradedBy())
                .gradeReleased(Boolean.TRUE.equals(submission.getGradeReleased()))
                .returnedAt(submission.getReturnedAt())
                .createdAt(submission.getCreatedAt())
                .updatedAt(submission.getUpdatedAt())
                .build();
    }

    private String fullName(StudentLookupResponse student) {
        String fullName = Stream.of(student.getFirstName(), student.getLastName())
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        return fullName.isBlank() ? null : fullName;
    }
}
