package com.mentify.controller;

import com.mentify.dto.UserResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;


    @GetMapping("/all-teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllTeachers(Pageable pageable){
        ApiResponse<Page<UserResponse>> response = teacherService.getAllTeachers(pageable);
        return new ResponseEntity<>(response,response.getStatus());
    }
}
