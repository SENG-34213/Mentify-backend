package com.mentify.service;

import com.mentify.dto.UserResponse;
import com.mentify.entity.User;
import com.mentify.enums.Role;
import com.mentify.mapper.TeacherMapper;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.UserRepository;
import com.mentify.service.impl.TeacherServiceImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeacherMapper teacherMapper;

    private TeacherServiceImpl teacherServiceImpl;

    @BeforeEach
    void setUp() {
        teacherServiceImpl = new TeacherServiceImpl(userRepository, teacherMapper);
    }

    @Test
    void getAllTeachers_whenTeachersExist_returnsPagedTeacherResponses() {
        Pageable pageable = PageRequest.of(0, 10);

        User teacher = User.builder()
                .email("teacher@mentify.com")
                .firstName("John")
                .lastName("Smith")
                .role(Role.TEACHER)
                .build();

        Page<User> teacherPage = new PageImpl<>(List.of(teacher), pageable, 1);

        UserResponse teacherResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .email(teacher.getEmail())
                .role(Role.TEACHER)
                .build();

        when(userRepository.findAllByRole(eq(Role.TEACHER), eq(pageable))).thenReturn(teacherPage);
        when(teacherMapper.toUserResponse(teacher)).thenReturn(teacherResponse);

        ApiResponse<Page<UserResponse>> response = teacherServiceImpl.getAllTeachers(pageable);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData().getContent()).hasSize(1);
        assertThat(response.getData().getContent().get(0).getEmail()).isEqualTo("teacher@mentify.com");
        assertThat(response.getData().getTotalElements()).isEqualTo(1);

        verify(userRepository).findAllByRole(eq(Role.TEACHER), eq(pageable));
    }

    @Test
    void getAllTeachers_whenNoTeachersExist_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findAllByRole(eq(Role.TEACHER), eq(pageable))).thenReturn(emptyPage);

        ApiResponse<Page<UserResponse>> response = teacherServiceImpl.getAllTeachers(pageable);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getData().getContent()).isEmpty();
        assertThat(response.getData().getTotalElements()).isZero();
    }
}