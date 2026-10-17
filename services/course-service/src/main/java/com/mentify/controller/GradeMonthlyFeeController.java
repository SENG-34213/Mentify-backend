package com.mentify.controller;

import com.mentify.dto.GradeMonthlyFeeRequest;
import com.mentify.dto.GradeMonthlyFeeResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.GradeMonthlyFeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/course/grades")
@RequiredArgsConstructor
public class GradeMonthlyFeeController {

    private final GradeMonthlyFeeService service;

    @GetMapping("/monthly-fees")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<GradeMonthlyFeeResponse>>> getAll() {
        ApiResponse<List<GradeMonthlyFeeResponse>> response = service.getAll();
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/{gradeId}/monthly-fee")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GradeMonthlyFeeResponse>> getByGrade(@PathVariable UUID gradeId) {
        ApiResponse<GradeMonthlyFeeResponse> response = service.getByGrade(gradeId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/{gradeId}/monthly-fee")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GradeMonthlyFeeResponse>> create(
            @PathVariable UUID gradeId, @Valid @RequestBody GradeMonthlyFeeRequest request
    ) {
        ApiResponse<GradeMonthlyFeeResponse> response = service.create(gradeId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{gradeId}/monthly-fee")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GradeMonthlyFeeResponse>> update(
            @PathVariable UUID gradeId, @Valid @RequestBody GradeMonthlyFeeRequest request
    ) {
        ApiResponse<GradeMonthlyFeeResponse> response = service.update(gradeId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/{gradeId}/monthly-fee")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable UUID gradeId) {
        ApiResponse<Object> response = service.delete(gradeId);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
