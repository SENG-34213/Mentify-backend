package com.mentify.service.impl;

import com.mentify.dto.PaginatedStudentsResponse;
import com.mentify.dto.StudentRegistrationRequest;
import com.mentify.dto.StudentSummaryResponse;
import com.mentify.dto.UserResponse;
import com.mentify.entity.Address;
import com.mentify.entity.StudentProfile;
import com.mentify.entity.User;
import com.mentify.enums.Role;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.mapper.StudentMapper;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.UserRepository;
import com.mentify.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final UserRepository userRepository;
    private final StudentMapper studentMapper;

    @Override
    @Transactional
    public ApiResponse<UserResponse> registerStudent(StudentRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("User", "email", request.getEmail());
        }

        User user = studentMapper.toUserEntity(request);
        StudentProfile profile = studentMapper.toStudentProfileEntity(request);
        Address address = studentMapper.toAddressEntity(request.getAddress());

        user.setStudentProfile(profile);
        user.setAddress(address);

        User savedUser = userRepository.save(user);

        UserResponse userResponse = studentMapper.toUserResponse(savedUser);

        return ApiResponse.<UserResponse>builder()
                .message("Student registered successfully")
                .data(userResponse)
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedStudentsResponse getAllStudents(Pageable pageable) {
        Page<User> studentPage = userRepository.findByRole(Role.STUDENT, pageable);

        return PaginatedStudentsResponse.builder()
                .content(studentPage.getContent().stream()
                        .map(student -> StudentSummaryResponse.builder()
                                .id(student.getId())
                                .firstName(student.getFirstName())
                                .lastName(student.getLastName())
                                .email(student.getEmail())
                                .build())
                        .toList())
                .pageNumber(studentPage.getNumber())
                .pageSize(studentPage.getSize())
                .totalElements(studentPage.getTotalElements())
                .totalPages(studentPage.getTotalPages())
                .build();
    }
}
