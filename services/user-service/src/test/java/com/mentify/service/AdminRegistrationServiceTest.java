package com.mentify.service;

import com.mentify.dto.AddressDto;
import com.mentify.dto.SuperAdminRegisterAdminRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import com.mentify.exception.DuplicateResourceException;
import com.mentify.exception.KeycloakRoleAssignmentException;
import com.mentify.repository.AdminProfileRepository;
import com.mentify.repository.UserRepository;
import com.mentify.repository.TeacherProfileRepository;
import com.mentify.service.registration.AdminCodeGenerator;
import com.mentify.service.registration.KeycloakUserProvisionRequest;
import com.mentify.service.registration.PasswordSetupEmailDispatcher;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private StudentIdGenerator studentIdGenerator;

    @Mock
    private TeacherProfileRepository teacherProfileRepository;

    @Mock
    private AdminProfileRepository adminProfileRepository;

    @Mock
    private PasswordSetupEmailDispatcher passwordSetupEmailDispatcher;

    private AdminRegistrationService adminRegistrationService;

    @BeforeEach
    void setUp() {
        UserRegistrationFactory factory = new UserRegistrationFactory(
                studentIdGenerator,
                new TeacherCodeGenerator(teacherProfileRepository),
                new AdminCodeGenerator(adminProfileRepository)
        );

        adminRegistrationService = new AdminRegistrationService(
                userRepository,
                keycloakUserService,
                factory,
                passwordSetupEmailDispatcher
        );
    }

    @Test
    void registerAdmin_whenRequestIsValid_createsAdminUser() {
        SuperAdminRegisterAdminRequest request = validRequest();
        String keycloakUserId = "keycloak-admin-id";
        UUID localUserId = UUID.randomUUID();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(keycloakUserService.createUser(any(KeycloakUserProvisionRequest.class))).thenReturn(keycloakUserId);
        when(adminProfileRepository.findLastAdminNumber()).thenReturn(4);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(localUserId);
            return user;
        });

        UserRegistrationResponse response = adminRegistrationService.registerAdmin(request);

        assertThat(response.getId()).isEqualTo(localUserId);
        assertThat(response.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.getAccountStatus()).isEqualTo(AccountStatus.INVITED);
        assertThat(response.getAdminProfile()).isNotNull();
        assertThat(response.getAdminProfile().getAdminCode()).isEqualTo("TIT-ADM-005");

        verify(keycloakUserService).assignRealmRole(keycloakUserId, "ADMIN");
        verify(passwordSetupEmailDispatcher).sendAfterCommit(keycloakUserId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(savedUser.getPassword()).isNull();
        assertThat(savedUser.getAdminProfile()).isNotNull();
        assertThat(savedUser.getAdminProfile().getNic()).isEqualTo(request.getAdminProfile().getNic());
        assertThat(savedUser.getAddress()).isNotNull();
        assertThat(savedUser.getAddress().getCity()).isEqualTo(request.getAddress().getCity());
    }

    @Test
    void registerAdmin_whenEmailAlreadyExists_throwsDuplicateResourceException() {
        SuperAdminRegisterAdminRequest request = validRequest();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> adminRegistrationService.registerAdmin(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists");

        verify(keycloakUserService, never()).createUser(any());
    }

    @Test
    void registerAdmin_whenRoleAssignmentFails_deletesCreatedKeycloakUser() {
        SuperAdminRegisterAdminRequest request = validRequest();
        String keycloakUserId = "keycloak-admin-id";

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(keycloakUserService.createUser(any(KeycloakUserProvisionRequest.class))).thenReturn(keycloakUserId);
        org.mockito.Mockito.doThrow(new KeycloakRoleAssignmentException("Role missing"))
                .when(keycloakUserService)
                .assignRealmRole(keycloakUserId, "ADMIN");

        assertThatThrownBy(() -> adminRegistrationService.registerAdmin(request))
                .isInstanceOf(KeycloakRoleAssignmentException.class);

        verify(keycloakUserService).deleteUser(keycloakUserId);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerAdmin_whenDatabaseSaveFails_deletesCreatedKeycloakUser() {
        SuperAdminRegisterAdminRequest request = validRequest();
        String keycloakUserId = "keycloak-admin-id";

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(keycloakUserService.createUser(any(KeycloakUserProvisionRequest.class))).thenReturn(keycloakUserId);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("DB failure"));

        assertThatThrownBy(() -> adminRegistrationService.registerAdmin(request))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(keycloakUserService).deleteUser(keycloakUserId);
    }

    private SuperAdminRegisterAdminRequest validRequest() {
        return new SuperAdminRegisterAdminRequest(
                "admin@gmail.com",
                "Admin",
                "User",
                "0771234567",
                new SuperAdminRegisterAdminRequest.AdminProfileRequest(
                        LocalDate.of(1990, 5, 12),
                        "901234567V"
                ),
                AddressDto.builder()
                        .addressLine1("No 20")
                        .addressLine2("Main Road")
                        .city("Colombo")
                        .district("Colombo")
                        .postalCode("00100")
                        .build()
        );
    }
}
