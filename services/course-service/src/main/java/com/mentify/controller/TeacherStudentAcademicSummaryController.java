package com.mentify.controller;

import com.mentify.dto.TeacherStudentAcademicSummaryResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.TeacherStudentAcademicSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/students")
@RequiredArgsConstructor
public class TeacherStudentAcademicSummaryController {

    private final TeacherStudentAcademicSummaryService summaryService;

    @GetMapping("/{studentId}/summary")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TeacherStudentAcademicSummaryResponse>> getSummary(
            @PathVariable UUID studentId,
            @RequestParam UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader) {
        ApiResponse<TeacherStudentAcademicSummaryResponse> response = summaryService
                .getSummary(studentId, courseId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
