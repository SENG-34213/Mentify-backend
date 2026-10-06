package com.mentify.exam.controller;

import com.mentify.exam.dto.request.CreateExamRequest;
import com.mentify.exam.dto.request.UpdateExamRequest;
import com.mentify.exam.dto.response.ExamResponse;
import com.mentify.exam.service.ExamService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER')")
public class ExamController {

    private final ExamService examService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExamResponse>> createExam(
            @Valid @RequestBody CreateExamRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        return respond(HttpStatus.CREATED, "Exam created successfully",
                examService.createExam(request, authorizationHeader));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<ApiResponse<ExamResponse>> getExam(
            @PathVariable UUID examId,
            @RequestHeader("Authorization") String authorizationHeader) {
        return respond(HttpStatus.OK, "Exam fetched successfully",
                examService.getExam(examId, authorizationHeader));
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getExamsByCourse(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader) {
        return respond(HttpStatus.OK, "Exams fetched successfully",
                examService.getExamsByCourse(courseId, authorizationHeader));
    }

    @PutMapping("/{examId}")
    public ResponseEntity<ApiResponse<ExamResponse>> updateExam(
            @PathVariable UUID examId,
            @Valid @RequestBody UpdateExamRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        return respond(HttpStatus.OK, "Exam updated successfully",
                examService.updateExam(examId, request, authorizationHeader));
    }

    @PostMapping("/{examId}/cancel")
    public ResponseEntity<ApiResponse<ExamResponse>> cancelExam(
            @PathVariable UUID examId,
            @RequestHeader("Authorization") String authorizationHeader) {
        return respond(HttpStatus.OK, "Exam cancelled successfully",
                examService.cancelExam(examId, authorizationHeader));
    }

    private <T> ResponseEntity<ApiResponse<T>> respond(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status).body(ApiResponse.success(status.value(), message, data));
    }
}
