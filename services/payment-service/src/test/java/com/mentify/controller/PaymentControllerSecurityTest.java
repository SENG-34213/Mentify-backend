package com.mentify.controller;

import com.mentify.common.security.KeycloakJwtAuthenticationConverter;
import com.mentify.common.security.KeycloakRoleConverter;
import com.mentify.common.security.SecurityConfig;
import com.mentify.dto.AdminPaymentDetailResponse;
import com.mentify.dto.PaymentVerificationResponse;
import com.mentify.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PaymentController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "mentify.security.enabled=true"
        }
)
@Import({SecurityConfig.class, KeycloakJwtAuthenticationConverter.class, KeycloakRoleConverter.class})
class PaymentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void givenAdminRole_whenGettingAdminPayments_thenReturnsOk() throws Exception {
        when(paymentService.getAdminPayments(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/payments/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void givenSuperAdminRole_whenGettingAdminPayments_thenReturnsOk() throws Exception {
        when(paymentService.getAdminPayments(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/payments/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void givenStudentRole_whenGettingAdminPayments_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/payments/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(paymentService);
    }

    @Test
    void givenTeacherRole_whenGettingAdminPayments_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/payments/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TEACHER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(paymentService);
    }

    @Test
    void givenAdminRole_whenGettingAdminPaymentDetail_thenReturnsOk() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(paymentService.getAdminPayment(paymentId)).thenReturn(AdminPaymentDetailResponse.builder()
                .paymentId(paymentId)
                .build());

        mockMvc.perform(get("/api/v1/payments/admin/{paymentId}", paymentId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void givenStudentRole_whenGettingAdminPaymentDetail_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/payments/admin/{paymentId}", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(paymentService);
    }

    @Test
    void givenStudentRole_whenVerifyingSuccessfulPayment_thenReturnsOk() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(paymentService.verifySuccessfulPayment(studentId, courseId)).thenReturn(PaymentVerificationResponse.builder()
                .studentId(studentId)
                .courseId(courseId)
                .successfulPaymentExists(true)
                .build());

        mockMvc.perform(get("/api/v1/payments/internal/students/{studentId}/courses/{courseId}/successful", studentId, courseId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk());
    }

    @Test
    void givenTeacherRole_whenVerifyingSuccessfulPayment_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/payments/internal/students/{studentId}/courses/{courseId}/successful",
                        UUID.randomUUID(),
                        UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TEACHER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(paymentService);
    }
}
