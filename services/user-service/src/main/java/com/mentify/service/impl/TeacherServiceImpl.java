package com.mentify.service.impl;

import com.mentify.dto.UserResponse;
import com.mentify.entity.User;
import com.mentify.enums.Role;
import com.mentify.mapper.TeacherMapper;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.UserRepository;
import com.mentify.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final UserRepository userRepository;
    private final TeacherMapper teacherMapper;

    @Override
    public ApiResponse<Page<UserResponse>> getAllTeachers(Pageable pageable) {

        Page<User> students = userRepository.findAllByRole(Role.STUDENT, pageable);
        Page<UserResponse> listOfStudents = students.map(teacherMapper::toUserResponse);

        return ApiResponse.<Page<UserResponse>>builder()
                .message("Successfully returned paginated list of Students")
                .data(listOfStudents)
                .status(HttpStatus.OK)
                .build();
    }
}
