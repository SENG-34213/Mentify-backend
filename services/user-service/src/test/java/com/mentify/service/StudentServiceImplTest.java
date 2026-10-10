package com.mentify.service;

import com.mentify.dto.UserResponse;
import com.mentify.entity.User;
import com.mentify.enums.Role;
import com.mentify.mapper.StudentMapper;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.UserRepository;
import com.mentify.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentMapper studentMapper;

    private StudentServiceImpl studentServiceImpl;

    @BeforeEach
    void setUp() {
        studentServiceImpl = new StudentServiceImpl(userRepository, studentMapper);
    }

    @Test
    void getAllStudents_whenStudentsExist_returnsPagedStudentResponses() {
        Pageable pageable = PageRequest.of(0, 10);

        User student = User.builder()
                .email("student@mentify.com")
                .firstName("Jane")
                .lastName("Doe")
                .role(Role.STUDENT)
                .build();

        Page<User> studentPage = new PageImpl<>(List.of(student), pageable, 1);

        UserResponse studentResponse = UserResponse.builder()
                .id(student.getId())
                .email(student.getEmail())
                .role(Role.STUDENT)
                .build();

        when(userRepository.findAllByRole(eq(Role.STUDENT), eq(pageable))).thenReturn(studentPage);
        when(studentMapper.toUserResponse(student)).thenReturn(studentResponse);

        ApiResponse<Page<UserResponse>> response = studentServiceImpl.getAllStudents(pageable);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData().getContent()).hasSize(1);
        assertThat(response.getData().getContent().get(0).getEmail()).isEqualTo("student@mentify.com");
        assertThat(response.getData().getTotalElements()).isEqualTo(1);

        verify(userRepository).findAllByRole(eq(Role.STUDENT), eq(pageable));
    }

    @Test
    void getAllStudents_whenNoStudentsExist_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findAllByRole(eq(Role.STUDENT), eq(pageable))).thenReturn(emptyPage);

        ApiResponse<Page<UserResponse>> response = studentServiceImpl.getAllStudents(pageable);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData().getContent()).isEmpty();
        assertThat(response.getData().getTotalElements()).isZero();
    }
}