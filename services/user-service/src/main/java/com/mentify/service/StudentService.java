package com.mentify.service;

import com.mentify.dto.PaginatedStudentsResponse;
import com.mentify.dto.StudentRegistrationRequest;
import com.mentify.dto.UserResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.data.domain.Pageable;

public interface StudentService {
    ApiResponse<UserResponse> registerStudent(StudentRegistrationRequest request);
    PaginatedStudentsResponse getAllStudents(Pageable pageable);
}
