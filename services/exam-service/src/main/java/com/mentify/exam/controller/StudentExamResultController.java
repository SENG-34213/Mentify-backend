package com.mentify.exam.controller;

import com.mentify.exam.dto.response.StudentExamResultsResponse;
import com.mentify.exam.service.StudentExamResultService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exams/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamResultController {

    private final StudentExamResultService studentExamResultService;

    @GetMapping
    public ResponseEntity<ApiResponse<StudentExamResultsResponse>> getMyResults() {
        return ResponseEntity.ok(ApiResponse.success(200, "Exam results fetched successfully",
                studentExamResultService.getMyResults()));
    }
}