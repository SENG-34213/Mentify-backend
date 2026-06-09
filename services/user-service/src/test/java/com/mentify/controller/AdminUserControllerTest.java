package com.mentify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.common.security.KeycloakJwtAuthenticationConverter;
import com.mentify.common.security.KeycloakRoleConverter;
import com.mentify.common.security.SecurityConfig;
import com.mentify.dto.AddressDto;
import com.mentify.dto.AdminRegisterUserRequest;
import com.mentify.dto.UserRegistrationResponse;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.AttendanceMode;
import com.mentify.enums.Role;
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
        controllers = AdminUserController.class,
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

    @Test
    void registerUser_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(Role.STUDENT))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userRegistrationService);
    }

    @Test
    void registerUser_whenStudentRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/register")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(Role.STUDENT))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userRegistrationService);
    }

    @Test
    void registerUser_whenTeacherRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/register")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(Role.STUDENT))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userRegistrationService);
    }

    @Test
    void registerUser_whenAdminRole_returnsCreated() throws Exception {
        UserRegistrationResponse response = registrationResponse(Role.STUDENT);
        when(userRegistrationService.registerUser(any(AdminRegisterUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users/register")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(Role.STUDENT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("User registered successfully. Password setup email sent."))
                .andExpect(jsonPath("$.data.email").value("student@gmail.com"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.accountStatus").value("INVITED"));

        verify(userRegistrationService).registerUser(any(AdminRegisterUserRequest.class));
    }

    @Test
    void registerUser_whenSuperAdminRole_returnsCreated() throws Exception {
        UserRegistrationResponse response = registrationResponse(Role.TEACHER);
        when(userRegistrationService.registerUser(any(AdminRegisterUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users/register")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(Role.TEACHER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("TEACHER"));

        verify(userRegistrationService).registerUser(any(AdminRegisterUserRequest.class));
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
                "Mathematics",
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
                null
        );
    }
}
