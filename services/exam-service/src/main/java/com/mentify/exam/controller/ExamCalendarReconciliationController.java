package com.mentify.exam.controller;

import com.mentify.exam.service.ExamCalendarProjectionService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/exams")
@RequiredArgsConstructor
public class ExamCalendarReconciliationController {

    private final ExamCalendarProjectionService projectionService;

    @PostMapping("/{examId}/calendar-reconcile")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reconcile(
            @PathVariable UUID examId,
            @RequestHeader("Authorization") String authorizationHeader) {
        projectionService.reconcile(examId, authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(200,
                "Exam calendar projection reconciliation completed", null));
    }
}
