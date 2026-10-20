package com.mentify.assignment.service;

import com.mentify.assignment.dto.response.StudentAssignmentResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.UUID;

public interface StudentAssignmentService {
    Page<StudentAssignmentResponse> getAssignments(
            UUID studentId, UUID courseId, String status,
            LocalDateTime dueFrom, LocalDateTime dueTo,
            int page, int size, String authorizationHeader);
}
