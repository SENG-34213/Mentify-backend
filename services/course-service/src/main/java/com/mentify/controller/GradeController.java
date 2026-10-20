package com.mentify.controller;

import com.mentify.dto.GradeRequest;
import com.mentify.dto.GradeResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.GradeService;
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
public class GradeController {

    private final GradeService gradeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GradeResponse>> createGrade(@Valid @RequestBody GradeRequest request) {
        ApiResponse<GradeResponse> response = gradeService.createGrade(request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{gradeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GradeResponse>> updateGrade(
            @PathVariable UUID gradeId,
            @Valid @RequestBody GradeRequest request
    ) {
        ApiResponse<GradeResponse> response = gradeService.updateGrade(gradeId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/{gradeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GradeResponse>> getGradeById(@PathVariable UUID gradeId) {
        ApiResponse<GradeResponse> response = gradeService.getGradeById(gradeId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<GradeResponse>>> getAllGrades() {
        ApiResponse<List<GradeResponse>> response = gradeService.getAllGrades();
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/{gradeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteGrade(@PathVariable UUID gradeId) {
        ApiResponse<Object> response = gradeService.deleteGrade(gradeId);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
