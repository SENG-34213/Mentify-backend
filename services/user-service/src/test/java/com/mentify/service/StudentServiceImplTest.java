package com.mentify.service;

import com.mentify.dto.PaginatedStudentsResponse;
import com.mentify.entity.User;
import com.mentify.enums.Role;
import com.mentify.mapper.StudentMapper;
import com.mentify.repository.UserRepository;
import com.mentify.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentMapper studentMapper;

    @Test
    void getAllStudents_returnsMappedPaginatedResponse() {
        StudentServiceImpl studentService = new StudentServiceImpl(userRepository, studentMapper);
        Pageable pageable = PageRequest.of(0, 2);

        User studentOne = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .role(Role.STUDENT)
                .build();
        studentOne.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        User studentTwo = User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .role(Role.STUDENT)
                .build();
        studentTwo.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

        Page<User> studentsPage = new PageImpl<>(List.of(studentOne, studentTwo), pageable, 4);
        when(userRepository.findByRole(Role.STUDENT, pageable)).thenReturn(studentsPage);

        PaginatedStudentsResponse response = studentService.getAllStudents(pageable);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getId()).isEqualTo(studentOne.getId());
        assertThat(response.getContent().get(0).getFirstName()).isEqualTo("John");
        assertThat(response.getContent().get(0).getLastName()).isEqualTo("Doe");
        assertThat(response.getContent().get(0).getEmail()).isEqualTo("john@example.com");
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(4);
        assertThat(response.getTotalPages()).isEqualTo(2);
        verify(userRepository).findByRole(Role.STUDENT, pageable);
    }

    @Test
    void getAllStudents_whenNoStudents_returnsEmptyResponse() {
        StudentServiceImpl studentService = new StudentServiceImpl(userRepository, studentMapper);
        Pageable pageable = PageRequest.of(0, 10);

        Page<User> studentsPage = new PageImpl<>(List.of(), pageable, 0);
        when(userRepository.findByRole(Role.STUDENT, pageable)).thenReturn(studentsPage);

        PaginatedStudentsResponse response = studentService.getAllStudents(pageable);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(0);
        verify(userRepository).findByRole(Role.STUDENT, pageable);
    }
}
