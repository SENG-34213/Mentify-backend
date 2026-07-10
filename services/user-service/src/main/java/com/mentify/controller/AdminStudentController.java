package com.mentify.controller;

import com.mentify.dto.PaginatedStudentsResponse;
import com.mentify.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/v1/admin/students")
@RequiredArgsConstructor
@Tag(name = "Admin Students")
public class AdminStudentController {

    private final StudentService studentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all students with pagination")
    public ResponseEntity<PaginatedStudentsResponse> getAllStudents(
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(studentService.getAllStudents(pageable));
    }
}
