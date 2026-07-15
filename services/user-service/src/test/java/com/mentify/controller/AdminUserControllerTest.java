package com.mentify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.common.security.KeycloakJwtAuthenticationConverter;
import com.mentify.common.security.KeycloakRoleConverter;
import com.mentify.common.security.SecurityConfig;
import com.mentify.dto.AddressDto;
import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.dto.SuperAdminRegisterAdminRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.AttendanceMode;
import com.mentify.enums.Role;
import com.mentify.service.AdminRegistrationService;
import com.mentify.service.UserRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserRegistration.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "mentify.security.enabled=true"
        }
)
@Import({SecurityConfig.class, KeycloakJwtAuthenticationConverter.class, KeycloakRoleConverter.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private UserRegistrationService userRegistrationService;

    @MockBean
    private AdminRegistrationService adminRegistrationService;

    @Test
    void registerUser_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(Role.STUDENT))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userRegistrationService, adminRegistrationService);
    }

    @Test
    void registerUser_whenStudentRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users/register")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(Role.STUDENT))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userRegistrationService, adminRegistrationService);
    }

    @Test
    void registerUser_whenTeacherRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users/register")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(Role.STUDENT))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userRegistrationService, adminRegistrationService);
    }

    @Test
    void registerUser_whenAdminRole_returnsCreated() throws Exception {
        UserRegistrationResponse response = registrationResponse(Role.STUDENT);
        when(userRegistrationService.registerUser(any(AdminRegisterUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/register")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(Role.STUDENT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("User registered successfully. Password setup email will be sent."))
                .andExpect(jsonPath("$.data.email").value("student@gmail.com"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.accountStatus").value("INVITED"));

        verify(userRegistrationService).registerUser(any(AdminRegisterUserRequest.class));
    }

    @Test
    void registerUser_whenSuperAdminRole_returnsCreated() throws Exception {
        UserRegistrationResponse response = registrationResponse(Role.TEACHER);
        when(userRegistrationService.registerUser(any(AdminRegisterUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/register")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(Role.TEACHER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("TEACHER"));

        verify(userRegistrationService).registerUser(any(AdminRegisterUserRequest.class));
    }

    @Test
    void resendInvitation_whenUnauthenticated_returnsUnauthorized() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/users/{userId}/resend-invitation", userId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userRegistrationService, adminRegistrationService);
    }

    @Test
    void resendInvitation_whenStudentRole_returnsForbidden() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/users/{userId}/resend-invitation", userId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userRegistrationService, adminRegistrationService);
    }

    @Test
    void resendInvitation_whenAdminRole_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/users/{userId}/resend-invitation", userId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Invitation email sent successfully."))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userRegistrationService).resendInvitation(userId);
    }

    @Test
    void resendInvitation_whenSuperAdminRole_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/users/{userId}/resend-invitation", userId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation email sent successfully."));

        verify(userRegistrationService).resendInvitation(userId);
    }

    @Test
    void registerAdmin_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/users/admins/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAdminRequest())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userRegistrationService, adminRegistrationService);
    }

    @Test
    void registerAdmin_whenAdminRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users/admins/register")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAdminRequest())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userRegistrationService, adminRegistrationService);
    }

    @Test
    void registerAdmin_whenSuperAdminRole_returnsCreated() throws Exception {
        UserRegistrationResponse response = registrationResponse(Role.ADMIN);
        when(adminRegistrationService.registerAdmin(any(SuperAdminRegisterAdminRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/admins/register")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAdminRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Admin registered successfully. Password setup email will be sent."))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        verify(adminRegistrationService).registerAdmin(any(SuperAdminRegisterAdminRequest.class));
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

    private SuperAdminRegisterAdminRequest validAdminRequest() {
        return new SuperAdminRegisterAdminRequest(
                "admin@gmail.com",
                "Admin",
                "User",
                "0771234567",
                new SuperAdminRegisterAdminRequest.AdminProfileRequest(
                        LocalDate.of(1990, 5, 12),
                        "901234567V"
                ),
                validAddress()
        );
    }

    private UserRegistrationResponse registrationResponse(Role role) {
        return new UserRegistrationResponse(
                UUID.randomUUID(),
                "keycloak-user-id",
                "student@gmail.com",
                "Kamal",
                "Perera",
                "0771234567",
                role,
                AccountStatus.INVITED,
                null,
                null,
                null,
                null
        );
    }
}
