package com.mentify.service;

import com.mentify.dto.StudentRegistrationRequest;
import com.mentify.dto.UserResponse;
import com.mentify.payload.response.ApiResponse;

public interface StudentService {
    ApiResponse<UserResponse> registerStudent(StudentRegistrationRequest request);
}
