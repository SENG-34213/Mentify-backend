package com.mentify.service;

import com.mentify.dto.UserResponse;
import com.mentify.dto.AddressDto;
import com.mentify.dto.StudentRegistrationRequest;
import com.mentify.entity.Address;
import com.mentify.entity.StudentProfile;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.AttendanceMode;
import com.mentify.enums.Role;
import com.mentify.mapper.StudentMapper;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.UserRepository;
import com.mentify.service.impl.StudentServiceImpl;
import com.mentify.service.KeycloakUserService;
import com.mentify.service.registration.KeycloakUserProvisionRequest;
import com.mentify.service.registration.RegistrationEmailDispatcher;
import com.mentify.service.registration.TemporaryPasswordGenerator;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private RegistrationEmailDispatcher registrationEmailDispatcher;

    private StudentServiceImpl studentServiceImpl;

    @BeforeEach
    void setUp() {
        studentServiceImpl = new StudentServiceImpl(
                userRepository,
                studentMapper,
                keycloakUserService,
                registrationEmailDispatcher,
                new TemporaryPasswordGenerator());
    }

    @Test
    void registerStudent_createsKeycloakStudentWithTemporaryCredentialAndDoesNotStoreItLocally() {
        StudentRegistrationRequest request = StudentRegistrationRequest.builder()
                .email("student@example.com")
                .password("legacy-client-password")
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(2010, 1, 1))
                .phoneNumber("0771234567")
                .guardianName("Parent")
                .guardianPhone("0771234568")
                .attendanceMode(AttendanceMode.ONLINE)
                .address(AddressDto.builder().addressLine1("1 Main Road").city("Colombo").build())
                .build();
        String keycloakUserId = "keycloak-student-id";
        User user = User.builder()
                .email(request.getEmail())
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserResponse userResponse = UserResponse.builder()
                .email(request.getEmail())
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.INVITED)
                .build();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(keycloakUserService.createUser(any(KeycloakUserProvisionRequest.class))).thenReturn(keycloakUserId);
        when(studentMapper.toUserEntity(request)).thenReturn(user);
        when(studentMapper.toStudentProfileEntity(request)).thenReturn(StudentProfile.builder().build());
        when(studentMapper.toAddressEntity(request.getAddress())).thenReturn(Address.builder().city("Colombo").build());
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(studentMapper.toUserResponse(user)).thenReturn(userResponse);

        ApiResponse<UserResponse> response = studentServiceImpl.registerStudent(request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(user.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.INVITED);
        assertThat(user.getPassword()).isNull();
        assertThat(user.isEmailVerified()).isTrue();
        verify(keycloakUserService).assignRealmRole(keycloakUserId, Role.STUDENT.name());
        verify(keycloakUserService).setInitialPassword(eq(keycloakUserId), anyString());
        verify(registrationEmailDispatcher).sendAfterCommit(
                eq(keycloakUserId), eq(request.getEmail()), eq(request.getFirstName()), anyString());
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
