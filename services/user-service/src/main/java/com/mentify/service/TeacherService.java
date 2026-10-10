package com.mentify.service;

import com.mentify.dto.UserResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeacherService {
    ApiResponse<Page<UserResponse>> getAllTeachers (Pageable pageable);
}
