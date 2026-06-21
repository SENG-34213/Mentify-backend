package com.mentify.controller;

import com.mentify.dto.StudentRegistrationRequest;
import com.mentify.dto.UserResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
        ApiResponse<UserResponse> response = studentService.registerStudent(request);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
