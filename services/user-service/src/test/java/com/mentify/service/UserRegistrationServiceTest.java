package com.mentify.service;

import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.dto.AddressDto;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.AttendanceMode;
import com.mentify.enums.Role;
import com.mentify.exception.DuplicateResourceException;
import com.mentify.exception.InvalidRoleException;
import com.mentify.exception.InvalidUserStateException;
import com.mentify.exception.KeycloakRoleAssignmentException;
import com.mentify.exception.KeycloakUserCreationException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.repository.AdminProfileRepository;
import com.mentify.repository.StudentProfileRepository;
import com.mentify.repository.TeacherProfileRepository;
import com.mentify.repository.UserRepository;
import com.mentify.service.registration.PasswordSetupEmailDispatcher;
import com.mentify.service.registration.AdminCodeGenerator;
import com.mentify.service.registration.KeycloakUserProvisionRequest;
import com.mentify.service.registration.RegistrationRequestValidator;
import com.mentify.service.registration.StudentIdGenerator;
import com.mentify.service.registration.TeacherCodeGenerator;
import com.mentify.service.registration.UserRegistrationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private TeacherProfileRepository teacherProfileRepository;

    @Mock
    private AdminProfileRepository adminProfileRepository;

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private PasswordSetupEmailDispatcher passwordSetupEmailDispatcher;

    private UserRegistrationService userRegistrationService;

    @BeforeEach
    void setUp() {
        RegistrationRequestValidator validator = new RegistrationRequestValidator();
        StudentIdGenerator studentIdGenerator = new StudentIdGenerator(studentProfileRepository);
        TeacherCodeGenerator teacherCodeGenerator = new TeacherCodeGenerator(teacherProfileRepository);
        AdminCodeGenerator adminCodeGenerator = new AdminCodeGenerator(adminProfileRepository);
        UserRegistrationFactory factory = new UserRegistrationFactory(studentIdGenerator, teacherCodeGenerator, adminCodeGenerator);
        userRegistrationService = new UserRegistrationService(
                userRepository,
                keycloakUserService,
                validator,
                factory,
                passwordSetupEmailDispatcher
        );
    }

    @Test
    void registerUser_whenStudentRequestIsValid_createsKeycloakUserAssignsRoleSendsEmailAndSavesProfile() {
        AdminRegisterUserRequest request = validRequest(Role.STUDENT);
        String keycloakUserId = "keycloak-student-id";
        UUID localUserId = UUID.randomUUID();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(keycloakUserService.createUser(any(KeycloakUserProvisionRequest.class))).thenReturn(keycloakUserId);
        when(studentProfileRepository.findLastStudentNumberByGrade("07")).thenReturn(23);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(localUserId);
            return user;
        });

        UserRegistrationResponse response = userRegistrationService.registerUser(request);

        assertThat(response.getId()).isEqualTo(localUserId);
        assertThat(response.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getRole()).isEqualTo(Role.STUDENT);
        assertThat(response.getAccountStatus()).isEqualTo(AccountStatus.INVITED);

        verify(keycloakUserService).assignRealmRole(keycloakUserId, "STUDENT");
        verify(passwordSetupEmailDispatcher).sendAfterCommit(keycloakUserId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isNull();
        assertThat(savedUser.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(savedUser.getLastName()).isEqualTo(request.getLastName());
        assertThat(savedUser.getPhoneNumber()).isEqualTo(request.getPhoneNumber());
        assertThat(savedUser.getStudentProfile()).isNotNull();
        assertThat(savedUser.getStudentProfile().getStudentId()).isEqualTo("TIT-07-24");
        assertThat(savedUser.getStudentProfile().getGrade()).isEqualTo("07");
        assertThat(savedUser.getStudentProfile().getGuardianName()).isEqualTo(request.getStudentProfile().getGuardianName());
        assertThat(savedUser.getStudentProfile().getGuardianPhone()).isEqualTo(request.getStudentProfile().getGuardianPhone());
        assertThat(savedUser.getStudentProfile().getAttendanceMode()).isEqualTo(request.getStudentProfile().getAttendanceMode());
        assertThat(savedUser.getAddress()).isNotNull();
        assertThat(savedUser.getAddress().getCity()).isEqualTo(request.getAddress().getCity());
    }

    @Test
    void registerUser_whenTeacherRequestIsValid_createsTeacherUser() {
        AdminRegisterUserRequest request = validRequest(Role.TEACHER);
        String keycloakUserId = "keycloak-teacher-id";

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(keycloakUserService.createUser(any(KeycloakUserProvisionRequest.class))).thenReturn(keycloakUserId);
        when(teacherProfileRepository.findLastTeacherNumber()).thenReturn(4);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRegistrationResponse response = userRegistrationService.registerUser(request);

        assertThat(response.getRole()).isEqualTo(Role.TEACHER);
        assertThat(response.getAccountStatus()).isEqualTo(AccountStatus.INVITED);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getTeacherProfile()).isNotNull();
        assertThat(savedUser.getTeacherProfile().getTeacherCode()).isEqualTo("TIT-TCH-005");
        assertThat(savedUser.getTeacherProfile().getNic()).isEqualTo(request.getTeacherProfile().getNic());
        assertThat(savedUser.getTeacherProfile().getSpecializations())
                .containsExactly("Mathematics", "Physics");
        assertThat(savedUser.getAddress()).isNotNull();
        assertThat(savedUser.getAddress().getCity()).isEqualTo(request.getAddress().getCity());

        verify(keycloakUserService).assignRealmRole(keycloakUserId, "TEACHER");
        verify(passwordSetupEmailDispatcher).sendAfterCommit(keycloakUserId);
    }

    @Test
    void registerUser_whenEmailAlreadyExists_throwsDuplicateResourceException() {
        AdminRegisterUserRequest request = validRequest(Role.STUDENT);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userRegistrationService.registerUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists");

        verify(keycloakUserService, never()).createUser(any());
    }

    @Test
    void registerUser_whenRoleIsAdmin_throwsInvalidRoleException() {
        AdminRegisterUserRequest request = validRequest(Role.ADMIN);

        assertThatThrownBy(() -> userRegistrationService.registerUser(request))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessage("Only STUDENT and TEACHER users can be registered from this endpoint");

        verify(userRepository, never()).existsByEmail(any());
        verify(keycloakUserService, never()).createUser(any());
    }

    @Test
    void registerUser_whenStudentProfileIsMissing_throwsInvalidRoleException() {
        AdminRegisterUserRequest request = new AdminRegisterUserRequest(
                "student@gmail.com",
                "Kamal",
                "Perera",
                "0771234567",
                Role.STUDENT,
                null,
                null,
                validAddress()
        );

        assertThatThrownBy(() -> userRegistrationService.registerUser(request))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessage("Student profile details are required when registering a STUDENT");

        verify(userRepository, never()).existsByEmail(any());
        verify(keycloakUserService, never()).createUser(any());
    }

    @Test
    void registerUser_whenTeacherProfileIsMissing_throwsInvalidRoleException() {
        AdminRegisterUserRequest request = new AdminRegisterUserRequest(
                "teacher@gmail.com",
                "Nimal",
                "Silva",
                "0771234567",
                Role.TEACHER,
                null,
                null,
                validAddress()
        );

        assertThatThrownBy(() -> userRegistrationService.registerUser(request))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessage("Teacher profile details are required when registering a TEACHER");

        verify(userRepository, never()).existsByEmail(any());
        verify(keycloakUserService, never()).createUser(any());
    }

    @Test
    void registerUser_whenAddressIsMissing_throwsInvalidRoleException() {
        AdminRegisterUserRequest request = new AdminRegisterUserRequest(
                "teacher@gmail.com",
                "Nimal",
                "Silva",
                "0771234567",
                Role.TEACHER,
                null,
                validTeacherProfile(),
                null
        );

        assertThatThrownBy(() -> userRegistrationService.registerUser(request))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessage("Address details are required when registering a user");

        verify(userRepository, never()).existsByEmail(any());
        verify(keycloakUserService, never()).createUser(any());
    }

    @Test
    void registerUser_whenKeycloakUserCreationFails_doesNotDeleteKeycloakUser() {
        AdminRegisterUserRequest request = validRequest(Role.STUDENT);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(keycloakUserService.createUser(any(KeycloakUserProvisionRequest.class)))
                .thenThrow(new KeycloakUserCreationException("Failed to create user in Keycloak"));

        assertThatThrownBy(() -> userRegistrationService.registerUser(request))
                .isInstanceOf(KeycloakUserCreationException.class);

        verify(keycloakUserService, never()).deleteUser(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerUser_whenKeycloakRoleAssignmentFails_deletesCreatedKeycloakUser() {
        AdminRegisterUserRequest request = validRequest(Role.STUDENT);
        String keycloakUserId = "keycloak-user-id";

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(keycloakUserService.createUser(any(KeycloakUserProvisionRequest.class))).thenReturn(keycloakUserId);
        org.mockito.Mockito.doThrow(new KeycloakRoleAssignmentException("Role missing"))
                .when(keycloakUserService)
                .assignRealmRole(keycloakUserId, "STUDENT");

        assertThatThrownBy(() -> userRegistrationService.registerUser(request))
                .isInstanceOf(KeycloakRoleAssignmentException.class);

        verify(keycloakUserService).deleteUser(keycloakUserId);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerUser_whenDatabaseSaveFails_deletesCreatedKeycloakUser() {
        AdminRegisterUserRequest request = validRequest(Role.STUDENT);
        String keycloakUserId = "keycloak-user-id";

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(keycloakUserService.createUser(any(KeycloakUserProvisionRequest.class))).thenReturn(keycloakUserId);
        when(studentProfileRepository.findLastStudentNumberByGrade("07")).thenReturn(23);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("DB failure"));

        assertThatThrownBy(() -> userRegistrationService.registerUser(request))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(keycloakUserService).deleteUser(keycloakUserId);
    }

    @Test
    void resendInvitation_whenUserIsInvited_sendsPasswordSetupEmail() {
        UUID userId = UUID.randomUUID();
        User user = invitedUser(userId, "keycloak-user-id");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userRegistrationService.resendInvitation(userId);

        verify(keycloakUserService).sendPasswordSetupEmail("keycloak-user-id");
    }

    @Test
    void resendInvitation_whenUserDoesNotExist_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userRegistrationService.resendInvitation(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(keycloakUserService, never()).sendPasswordSetupEmail(any());
    }

    @Test
    void resendInvitation_whenUserIsNotInvited_throwsInvalidUserStateException() {
        UUID userId = UUID.randomUUID();
        User user = invitedUser(userId, "keycloak-user-id");
        user.setAccountStatus(AccountStatus.ACTIVE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userRegistrationService.resendInvitation(userId))
                .isInstanceOf(InvalidUserStateException.class)
                .hasMessage("Invitation can only be resent for invited users");

        verify(keycloakUserService, never()).sendPasswordSetupEmail(any());
    }

    @Test
    void resendInvitation_whenUserIsNotLinkedToKeycloak_throwsInvalidUserStateException() {
        UUID userId = UUID.randomUUID();
        User user = invitedUser(userId, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userRegistrationService.resendInvitation(userId))
                .isInstanceOf(InvalidUserStateException.class)
                .hasMessage("User is not linked to Keycloak");

        verify(keycloakUserService, never()).sendPasswordSetupEmail(any());
    }

    private AdminRegisterUserRequest validRequest(Role role) {
        return new AdminRegisterUserRequest(
                "student@gmail.com",
                "Kamal",
                "Perera",
                "0771234567",
                role,
                role == Role.STUDENT ? validStudentProfile() : null,
                role == Role.TEACHER ? validTeacherProfile() : null,
                validAddress()
        );
    }

    private AdminRegisterUserRequest.StudentProfileRequest validStudentProfile() {
        return new AdminRegisterUserRequest.StudentProfileRequest(
                LocalDate.of(2010, 5, 12),
                "Sunil Perera",
                "0771112222",
                AttendanceMode.PHYSICAL,
                "7"
        );
    }

    private AdminRegisterUserRequest.TeacherProfileRequest validTeacherProfile() {
        return new AdminRegisterUserRequest.TeacherProfileRequest(
                LocalDate.of(1990, 2, 20),
                "901234567V",
                List.of("Mathematics", "Physics"),
                LocalDate.of(2026, 6, 9)
        );
    }

    private AddressDto validAddress() {
        return AddressDto.builder()
                .addressLine1("No 10")
                .addressLine2("Main Street")
                .city("Colombo")
                .district("Colombo")
                .postalCode("00100")
                .build();
    }

    private User invitedUser(UUID userId, String keycloakUserId) {
        User user = User.builder()
                .keycloakUserId(keycloakUserId)
                .email("student@gmail.com")
                .firstName("Kamal")
                .lastName("Perera")
                .role(Role.STUDENT)
                .accountStatus(AccountStatus.INVITED)
                .build();
        user.setId(userId);
        return user;
    }
}
