package com.mentify.controller;

import com.mentify.common.security.KeycloakJwtAuthenticationConverter;
import com.mentify.common.security.KeycloakRoleConverter;
import com.mentify.common.security.SecurityConfig;
import com.mentify.dto.PaginatedStudentsResponse;
import com.mentify.dto.StudentSummaryResponse;
import com.mentify.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminStudentController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "mentify.security.enabled=true"
        }
)
@Import({SecurityConfig.class, KeycloakJwtAuthenticationConverter.class, KeycloakRoleConverter.class})
class AdminStudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private StudentService studentService;

    @Test
    void getAllStudents_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/students"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(studentService);
    }

    @Test
    void getAllStudents_whenStudentRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/students")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(studentService);
    }

    @Test
    void getAllStudents_whenAdminRole_returnsPaginatedResponse() throws Exception {
        PaginatedStudentsResponse response = PaginatedStudentsResponse.builder()
                .content(List.of(StudentSummaryResponse.builder()
                        .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .firstName("John")
                        .lastName("Doe")
                        .email("john@example.com")
                        .build()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .build();
        when(studentService.getAllStudents(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/students")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.content[0].firstName").value("John"))
                .andExpect(jsonPath("$.content[0].lastName").value("Doe"))
                .andExpect(jsonPath("$.content[0].email").value("john@example.com"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(studentService).getAllStudents(any());
    }
}
