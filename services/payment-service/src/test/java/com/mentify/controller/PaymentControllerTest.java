package com.mentify.controller;

import com.mentify.dto.AdminPaymentDetailResponse;
import com.mentify.dto.AdminPaymentFilter;
import com.mentify.dto.AdminPaymentSummaryResponse;
import com.mentify.dto.PaymentDetailResponse;
import com.mentify.dto.PaymentStatusResponse;
import com.mentify.dto.PaymentSummaryResponse;
import com.mentify.dto.StartCoursePaymentRequest;
import com.mentify.dto.StartCoursePaymentResponse;
import com.mentify.enums.PaymentStatus;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentControllerTest {

    @Test
    void startCoursePayment_returnsCreatedApiResponse() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService);
        UUID courseId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String authorizationHeader = "Bearer student-token";
        StartCoursePaymentRequest request = StartCoursePaymentRequest.builder()
                .courseId(courseId)
                .build();
        StartCoursePaymentResponse serviceResponse = StartCoursePaymentResponse.builder()
                .paymentId(paymentId)
                .courseId(courseId)
                .amount(new BigDecimal("1000.00"))
                .currency("LKR")
                .status(PaymentStatus.PENDING)
                .clientSecret("client_secret")
                .build();
        when(paymentService.startCoursePayment(request, authorizationHeader)).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<StartCoursePaymentResponse>> response =
                controller.startCoursePayment(request, authorizationHeader);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.getBody().getData()).isEqualTo(serviceResponse);
        verify(paymentService).startCoursePayment(request, authorizationHeader);
    }

    @Test
    void getMyPayments_returnsOkApiResponse() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService);
        PaymentSummaryResponse serviceResponse = PaymentSummaryResponse.builder()
                .paymentId(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("LKR")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(paymentService.getCurrentStudentPayments()).thenReturn(List.of(serviceResponse));

        ResponseEntity<ApiResponse<List<PaymentSummaryResponse>>> response = controller.getMyPayments();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).containsExactly(serviceResponse);
        verify(paymentService).getCurrentStudentPayments();
    }

    @Test
    void getMyPayment_returnsOkApiResponse() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService);
        UUID paymentId = UUID.randomUUID();
        PaymentDetailResponse serviceResponse = PaymentDetailResponse.builder()
                .paymentId(paymentId)
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("LKR")
                .status(PaymentStatus.SUCCESS)
                .build();
        when(paymentService.getCurrentStudentPayment(paymentId)).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<PaymentDetailResponse>> response = controller.getMyPayment(paymentId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(serviceResponse);
        verify(paymentService).getCurrentStudentPayment(paymentId);
    }

    @Test
    void getMyPaymentStatus_returnsOkApiResponse() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService);
        UUID paymentId = UUID.randomUUID();
        PaymentStatusResponse serviceResponse = PaymentStatusResponse.builder()
                .paymentId(paymentId)
                .status(PaymentStatus.FAILED)
                .updatedAt(LocalDateTime.now())
                .build();
        when(paymentService.getCurrentStudentPaymentStatus(paymentId)).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<PaymentStatusResponse>> response = controller.getMyPaymentStatus(paymentId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(serviceResponse);
        verify(paymentService).getCurrentStudentPaymentStatus(paymentId);
    }

    @Test
    void getAdminPayments_returnsOkApiResponseWithFiltersAndPageable() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService);
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LocalDateTime createdFrom = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime createdTo = LocalDateTime.of(2026, 10, 10, 23, 59);
        PageRequest pageable = PageRequest.of(1, 5);
        AdminPaymentSummaryResponse adminPayment = AdminPaymentSummaryResponse.builder()
                .paymentId(UUID.randomUUID())
                .studentId(studentId)
                .courseId(courseId)
                .amount(new BigDecimal("1000.00"))
                .currency("LKR")
                .status(PaymentStatus.SUCCESS)
                .stripePaymentIntentId("pi_admin_001")
                .createdAt(LocalDateTime.now())
                .build();
        Page<AdminPaymentSummaryResponse> serviceResponse = new PageImpl<>(List.of(adminPayment), pageable, 1);
        when(paymentService.getAdminPayments(org.mockito.ArgumentMatchers.any(AdminPaymentFilter.class), org.mockito.ArgumentMatchers.eq(pageable)))
                .thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<Page<AdminPaymentSummaryResponse>>> response = controller.getAdminPayments(
                PaymentStatus.SUCCESS,
                studentId,
                courseId,
                createdFrom,
                createdTo,
                "pi_admin_001",
                pageable
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(serviceResponse);
        ArgumentCaptor<AdminPaymentFilter> filterCaptor = ArgumentCaptor.forClass(AdminPaymentFilter.class);
        verify(paymentService).getAdminPayments(filterCaptor.capture(), org.mockito.ArgumentMatchers.eq(pageable));
        assertThat(filterCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(filterCaptor.getValue().getStudentId()).isEqualTo(studentId);
        assertThat(filterCaptor.getValue().getCourseId()).isEqualTo(courseId);
        assertThat(filterCaptor.getValue().getCreatedFrom()).isEqualTo(createdFrom);
        assertThat(filterCaptor.getValue().getCreatedTo()).isEqualTo(createdTo);
        assertThat(filterCaptor.getValue().getReference()).isEqualTo("pi_admin_001");
    }

    @Test
    void getAdminPayment_returnsOkApiResponse() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService);
        UUID paymentId = UUID.randomUUID();
        AdminPaymentDetailResponse serviceResponse = AdminPaymentDetailResponse.builder()
                .paymentId(paymentId)
                .studentId(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("LKR")
                .status(PaymentStatus.SUCCESS)
                .stripePaymentIntentId("pi_admin_detail")
                .build();
        when(paymentService.getAdminPayment(paymentId)).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<AdminPaymentDetailResponse>> response = controller.getAdminPayment(paymentId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(serviceResponse);
        verify(paymentService).getAdminPayment(paymentId);
    }

    @Test
    void handleStripeWebhook_returnsAcceptedResponse() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService);

        ResponseEntity<ApiResponse<Object>> response = controller.handleStripeWebhook("payload", "signature");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Stripe webhook accepted");
        verify(paymentService).handleStripeWebhook("payload", "signature");
    }
}
