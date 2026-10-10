package com.mentify.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.client.CourseServiceClient;
import com.mentify.client.dto.CourseLookupResponse;
import com.mentify.config.StripeProperties;
import com.mentify.dto.AdminPaymentDetailResponse;
import com.mentify.dto.AdminPaymentFilter;
import com.mentify.dto.AdminPaymentSummaryResponse;
import com.mentify.dto.PaymentDetailResponse;
import com.mentify.dto.PaymentStatusResponse;
import com.mentify.dto.PaymentSummaryResponse;
import com.mentify.dto.PaymentVerificationResponse;
import com.mentify.dto.RefundPaymentRequest;
import com.mentify.dto.RefundPaymentResponse;
import com.mentify.dto.StartCoursePaymentRequest;
import com.mentify.dto.StartCoursePaymentResponse;
import com.mentify.entity.Payment;
import com.mentify.enums.PaymentStatus;
import com.mentify.exception.PaymentDomainException;
import com.mentify.exception.PaymentProviderException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.PaymentRepository;
import com.mentify.repository.ProcessedStripeWebhookEventRepository;
import com.mentify.security.AuthenticatedUserService;
import com.mentify.service.stripe.CreateStripePaymentIntentRequest;
import com.mentify.service.stripe.CreateStripeRefundRequest;
import com.mentify.service.stripe.StripePaymentIntent;
import com.mentify.service.stripe.StripePaymentIntentGateway;
import com.mentify.service.stripe.StripeRefund;
import com.mentify.service.stripe.StripeRefundGateway;
import com.mentify.service.stripe.StripeWebhookEvent;
import com.mentify.service.stripe.StripeWebhookVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    private static final String AUTHORIZATION_HEADER = "Bearer student-token";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProcessedStripeWebhookEventRepository processedStripeWebhookEventRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private AuthenticatedUserService currentUserService;

    @Mock
    private StripePaymentIntentGateway stripePaymentIntentGateway;

    @Mock
    private StripeRefundGateway stripeRefundGateway;

    @Mock
    private StripeWebhookVerifier stripeWebhookVerifier;

    private StripeProperties stripeProperties;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        stripeProperties = new StripeProperties();
        stripeProperties.setCurrency("LKR");
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                processedStripeWebhookEventRepository,
                courseServiceClient,
                currentUserService,
                stripePaymentIntentGateway,
                stripeRefundGateway,
                stripeWebhookVerifier,
                stripeProperties
        );

        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(UUID.randomUUID());
            }
            return payment;
        });
        when(paymentRepository.findFirstByPaymentOperationKeyAndStatusOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByStripePaymentIntentId(any())).thenReturn(Optional.empty());
        when(processedStripeWebhookEventRepository.insertIfAbsent(any(), any(), any(), any())).thenReturn(1);
    }

    @Test
    void startCoursePayment_whenCourseIsValid_createsPendingPaymentAndStripeIntent() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(courseServiceClient.getCourseById(courseId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", paidCourse(courseId, "1499.99")));
        when(stripePaymentIntentGateway.createPaymentIntent(any(CreateStripePaymentIntentRequest.class)))
                .thenReturn(new StripePaymentIntent("pi_123", "pi_123_secret_abc"));

        StartCoursePaymentResponse response = paymentService.startCoursePayment(
                StartCoursePaymentRequest.builder().courseId(courseId).build(),
                AUTHORIZATION_HEADER
        );

        assertThat(response.getPaymentId()).isNotNull();
        assertThat(response.getCourseId()).isEqualTo(courseId);
        assertThat(response.getAmount()).isEqualByComparingTo("1499.99");
        assertThat(response.getCurrency()).isEqualTo("LKR");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getClientSecret()).isEqualTo("pi_123_secret_abc");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).saveAndFlush(paymentCaptor.capture());
        Payment finalPayment = paymentCaptor.getAllValues().get(1);
        assertThat(finalPayment.getStudentId()).isEqualTo(studentId);
        assertThat(finalPayment.getCourseId()).isEqualTo(courseId);
        assertThat(finalPayment.getAmount()).isEqualByComparingTo("1499.99");
        assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(finalPayment.getStripePaymentIntentId()).isEqualTo("pi_123");
        assertThat(finalPayment.getStripeClientSecret()).isEqualTo("pi_123_secret_abc");

        ArgumentCaptor<CreateStripePaymentIntentRequest> stripeRequestCaptor =
                ArgumentCaptor.forClass(CreateStripePaymentIntentRequest.class);
        verify(stripePaymentIntentGateway).createPaymentIntent(stripeRequestCaptor.capture());
        CreateStripePaymentIntentRequest stripeRequest = stripeRequestCaptor.getValue();
        assertThat(stripeRequest.amountInMinorUnits()).isEqualTo(149999L);
        assertThat(stripeRequest.currency()).isEqualTo("LKR");
        assertThat(stripeRequest.studentId()).isEqualTo(studentId);
        assertThat(stripeRequest.courseId()).isEqualTo(courseId);
        assertThat(stripeRequest.paymentId()).isEqualTo(finalPayment.getId());
        assertThat(stripeRequest.idempotencyKey()).isEqualTo("payment-intent:" + finalPayment.getId());
    }

    @Test
    void startCoursePayment_whenMatchingPendingPaymentExists_returnsExistingStripeIntentWithoutCreatingDuplicate() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Payment existingPayment = pendingPayment("pi_existing_001");
        existingPayment.setStudentId(studentId);
        existingPayment.setCourseId(courseId);
        existingPayment.setAmount(new BigDecimal("1499.99"));
        existingPayment.setCurrency("LKR");
        existingPayment.setStripeClientSecret("pi_existing_secret");

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(courseServiceClient.getCourseById(courseId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", paidCourse(courseId, "1499.99")));
        when(paymentRepository.findFirstByPaymentOperationKeyAndStatusOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(existingPayment));

        StartCoursePaymentResponse response = paymentService.startCoursePayment(
                StartCoursePaymentRequest.builder().courseId(courseId).build(),
                AUTHORIZATION_HEADER
        );

        assertThat(response.getPaymentId()).isEqualTo(existingPayment.getId());
        assertThat(response.getClientSecret()).isEqualTo("pi_existing_secret");
        assertThat(response.getAmount()).isEqualByComparingTo("1499.99");
        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
        verify(stripePaymentIntentGateway, never()).createPaymentIntent(any(CreateStripePaymentIntentRequest.class));
    }

    @Test
    void startCoursePayment_validatesCourseBeforePaymentCreation() {
        UUID courseId = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(courseServiceClient.getCourseById(courseId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", paidCourse(courseId, "100.00")));
        when(stripePaymentIntentGateway.createPaymentIntent(any(CreateStripePaymentIntentRequest.class)))
                .thenReturn(new StripePaymentIntent("pi_123", "client_secret"));

        paymentService.startCoursePayment(
                StartCoursePaymentRequest.builder().courseId(courseId).build(),
                AUTHORIZATION_HEADER
        );

        InOrder inOrder = inOrder(courseServiceClient, paymentRepository, stripePaymentIntentGateway);
        inOrder.verify(courseServiceClient).getCourseById(courseId, AUTHORIZATION_HEADER);
        inOrder.verify(paymentRepository).saveAndFlush(any(Payment.class));
        inOrder.verify(stripePaymentIntentGateway).createPaymentIntent(any(CreateStripePaymentIntentRequest.class));
    }

    @Test
    void startCoursePayment_ignoresClientSuppliedIdentityAndAmount() throws Exception {
        UUID authenticatedStudentId = UUID.randomUUID();
        UUID spoofedStudentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        StartCoursePaymentRequest request = new ObjectMapper().readValue("""
                {
                  "courseId": "%s",
                  "studentId": "%s",
                  "amount": 1.00
                }
                """.formatted(courseId, spoofedStudentId), StartCoursePaymentRequest.class);

        when(currentUserService.getCurrentUserId()).thenReturn(authenticatedStudentId);
        when(courseServiceClient.getCourseById(courseId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", paidCourse(courseId, "2500.00")));
        when(stripePaymentIntentGateway.createPaymentIntent(any(CreateStripePaymentIntentRequest.class)))
                .thenReturn(new StripePaymentIntent("pi_trusted", "client_secret"));

        paymentService.startCoursePayment(request, AUTHORIZATION_HEADER);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).saveAndFlush(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getAllValues().get(1);
        assertThat(savedPayment.getStudentId()).isEqualTo(authenticatedStudentId);
        assertThat(savedPayment.getStudentId()).isNotEqualTo(spoofedStudentId);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo("2500.00");
    }

    @Test
    void startCoursePayment_whenCourseIsMissing_rejectsWithoutCreatingPayment() {
        UUID courseId = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(courseServiceClient.getCourseById(courseId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", null));

        assertThatThrownBy(() -> paymentService.startCoursePayment(
                StartCoursePaymentRequest.builder().courseId(courseId).build(),
                AUTHORIZATION_HEADER
        )).isInstanceOf(ResourceNotFoundException.class);

        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
        verify(stripePaymentIntentGateway, never()).createPaymentIntent(any(CreateStripePaymentIntentRequest.class));
    }

    @Test
    void startCoursePayment_whenCourseIsNotPublished_rejectsWithoutCreatingPayment() {
        UUID courseId = UUID.randomUUID();
        CourseLookupResponse course = paidCourse(courseId, "100.00");
        course.setPublished(false);
        when(currentUserService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(courseServiceClient.getCourseById(courseId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", course));

        assertThatThrownBy(() -> paymentService.startCoursePayment(
                StartCoursePaymentRequest.builder().courseId(courseId).build(),
                AUTHORIZATION_HEADER
        ))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessage("Course is not available for payment");

        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
    }

    @Test
    void startCoursePayment_whenStripeFails_marksLocalPaymentFailed() {
        UUID courseId = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(courseServiceClient.getCourseById(courseId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", paidCourse(courseId, "100.00")));
        when(stripePaymentIntentGateway.createPaymentIntent(any(CreateStripePaymentIntentRequest.class)))
                .thenThrow(new PaymentProviderException("Stripe unavailable"));

        assertThatThrownBy(() -> paymentService.startCoursePayment(
                StartCoursePaymentRequest.builder().courseId(courseId).build(),
                AUTHORIZATION_HEADER
        )).isInstanceOf(PaymentProviderException.class);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).saveAndFlush(paymentCaptor.capture());
        Payment failedPayment = paymentCaptor.getAllValues().get(1);
        assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failedPayment.getStripePaymentIntentId()).isNull();
    }

    @Test
    void startCoursePayment_whenStripeReferenceBelongsToAnotherPayment_rejectsSafely() {
        UUID courseId = UUID.randomUUID();
        Payment otherPayment = pendingPayment("pi_reused_reference");
        when(currentUserService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(courseServiceClient.getCourseById(courseId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", paidCourse(courseId, "100.00")));
        when(stripePaymentIntentGateway.createPaymentIntent(any(CreateStripePaymentIntentRequest.class)))
                .thenReturn(new StripePaymentIntent("pi_reused_reference", "client_secret"));
        when(paymentRepository.findByStripePaymentIntentId("pi_reused_reference")).thenReturn(Optional.of(otherPayment));

        assertThatThrownBy(() -> paymentService.startCoursePayment(
                StartCoursePaymentRequest.builder().courseId(courseId).build(),
                AUTHORIZATION_HEADER
        ))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessage("Stripe payment reference is already linked to another payment");
    }

    @Test
    void startCoursePayment_responseDoesNotExposeStripeSecretConfiguration() {
        UUID courseId = UUID.randomUUID();
        stripeProperties.setSecretKey("sk_test_should_not_be_returned");
        when(currentUserService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(courseServiceClient.getCourseById(courseId, AUTHORIZATION_HEADER))
                .thenReturn(ApiResponse.success(HttpStatus.OK.value(), "ok", paidCourse(courseId, "100.00")));
        when(stripePaymentIntentGateway.createPaymentIntent(any(CreateStripePaymentIntentRequest.class)))
                .thenReturn(new StripePaymentIntent("pi_123", "client_secret_only"));

        StartCoursePaymentResponse response = paymentService.startCoursePayment(
                StartCoursePaymentRequest.builder().courseId(courseId).build(),
                AUTHORIZATION_HEADER
        );

        assertThat(response.toString()).doesNotContain("sk_test_should_not_be_returned");
        assertThat(response.getClientSecret()).isEqualTo("client_secret_only");
    }

    @Test
    void getCurrentStudentPayments_whenStudentHasNoPayments_returnsEmptyList() {
        UUID studentId = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(paymentRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId)).thenReturn(List.of());

        List<PaymentSummaryResponse> response = paymentService.getCurrentStudentPayments();

        assertThat(response).isEmpty();
    }

    @Test
    void getCurrentStudentPayments_returnsOnlyRepositoryOrderedSafeDtos() {
        UUID studentId = UUID.randomUUID();
        Payment newestPayment = pendingPayment("pi_newest");
        newestPayment.setStudentId(studentId);
        newestPayment.setStripeClientSecret("secret_newest");
        newestPayment.setPaymentOperationKey("operation-newest");
        newestPayment.setCreatedAt(LocalDateTime.of(2026, 10, 10, 11, 0));

        Payment olderPayment = pendingPayment("pi_older");
        olderPayment.setStudentId(studentId);
        olderPayment.setStatus(PaymentStatus.SUCCESS);
        olderPayment.setStripeClientSecret("secret_older");
        olderPayment.setPaymentOperationKey("operation-older");
        olderPayment.setCreatedAt(LocalDateTime.of(2026, 10, 9, 11, 0));
        olderPayment.setPaidAt(LocalDateTime.of(2026, 10, 9, 11, 5));

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(paymentRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId))
                .thenReturn(List.of(newestPayment, olderPayment));

        List<PaymentSummaryResponse> response = paymentService.getCurrentStudentPayments();

        assertThat(response).extracting(PaymentSummaryResponse::getPaymentId)
                .containsExactly(newestPayment.getId(), olderPayment.getId());
        assertThat(response.get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.get(1).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.toString())
                .doesNotContain("pi_newest")
                .doesNotContain("secret_newest")
                .doesNotContain("operation-newest");
    }

    @Test
    void getCurrentStudentPayment_whenOwned_returnsSafeDetails() {
        UUID studentId = UUID.randomUUID();
        Payment payment = pendingPayment("pi_detail");
        payment.setStudentId(studentId);
        payment.setStripeClientSecret("secret_detail");
        payment.setPaymentOperationKey("operation-detail");
        payment.setCreatedAt(LocalDateTime.of(2026, 10, 10, 9, 0));
        payment.setUpdatedAt(LocalDateTime.of(2026, 10, 10, 9, 5));

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(paymentRepository.findByIdAndStudentId(payment.getId(), studentId)).thenReturn(Optional.of(payment));

        PaymentDetailResponse response = paymentService.getCurrentStudentPayment(payment.getId());

        assertThat(response.getPaymentId()).isEqualTo(payment.getId());
        assertThat(response.getCourseId()).isEqualTo(payment.getCourseId());
        assertThat(response.getAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.toString())
                .doesNotContain("pi_detail")
                .doesNotContain("secret_detail")
                .doesNotContain("operation-detail");
    }

    @Test
    void getCurrentStudentPayment_whenPaymentIsNotOwned_throwsNotFound() {
        UUID studentId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(paymentRepository.findByIdAndStudentId(paymentId, studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getCurrentStudentPayment(paymentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCurrentStudentPaymentStatus_whenOwned_returnsCurrentStatus() {
        UUID studentId = UUID.randomUUID();
        Payment payment = pendingPayment("pi_status");
        payment.setStudentId(studentId);
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.of(2026, 10, 10, 12, 0));

        when(currentUserService.getCurrentUserId()).thenReturn(studentId);
        when(paymentRepository.findByIdAndStudentId(payment.getId(), studentId)).thenReturn(Optional.of(payment));

        PaymentStatusResponse response = paymentService.getCurrentStudentPaymentStatus(payment.getId());

        assertThat(response.getPaymentId()).isEqualTo(payment.getId());
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 10, 12, 0));
        assertThat(response.toString()).doesNotContain("pi_status");
    }

    @Test
    void getAdminPayments_returnsPagedSafeDtosAndForcesRecentFirstOrdering() {
        Payment payment = pendingPayment("pi_admin_safe");
        payment.setStripeClientSecret("secret_admin");
        payment.setPaymentOperationKey("operation-admin");
        payment.setCreatedAt(LocalDateTime.of(2026, 10, 10, 12, 0));
        PageRequest requestedPageable = PageRequest.of(2, 5, Sort.by("createdAt").ascending());
        when(paymentRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Payment>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment), requestedPageable, 1));

        Page<AdminPaymentSummaryResponse> response = paymentService.getAdminPayments(
                AdminPaymentFilter.builder()
                        .status(PaymentStatus.PENDING)
                        .reference("pi_admin_safe")
                        .build(),
                requestedPageable
        );

        assertThat(response.getContent()).hasSize(1);
        AdminPaymentSummaryResponse dto = response.getContent().get(0);
        assertThat(dto.getPaymentId()).isEqualTo(payment.getId());
        assertThat(dto.getStudentId()).isEqualTo(payment.getStudentId());
        assertThat(dto.getCourseId()).isEqualTo(payment.getCourseId());
        assertThat(dto.getStripePaymentIntentId()).isEqualTo("pi_admin_safe");
        assertThat(dto.toString())
                .doesNotContain("secret_admin")
                .doesNotContain("operation-admin");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Payment>>any(), pageableCaptor.capture());
        Pageable actualPageable = pageableCaptor.getValue();
        assertThat(actualPageable.getPageNumber()).isEqualTo(2);
        assertThat(actualPageable.getPageSize()).isEqualTo(5);
        assertThat(actualPageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(actualPageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAdminPayments_whenDateRangeIsInvalid_throwsDomainException() {
        AdminPaymentFilter filter = AdminPaymentFilter.builder()
                .createdFrom(LocalDateTime.of(2026, 10, 11, 0, 0))
                .createdTo(LocalDateTime.of(2026, 10, 10, 0, 0))
                .build();

        assertThatThrownBy(() -> paymentService.getAdminPayments(filter, PageRequest.of(0, 10)))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessage("createdFrom must be before or equal to createdTo");

        verify(paymentRepository, never()).findAll(org.mockito.ArgumentMatchers.<Specification<Payment>>any(), any(Pageable.class));
    }

    @Test
    void getAdminPayment_whenPaymentExists_returnsSafeDetails() {
        Payment payment = pendingPayment("pi_admin_detail");
        payment.setStripeClientSecret("secret_detail");
        payment.setPaymentOperationKey("operation-detail");
        payment.setUpdatedAt(LocalDateTime.of(2026, 10, 10, 13, 0));
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        AdminPaymentDetailResponse response = paymentService.getAdminPayment(payment.getId());

        assertThat(response.getPaymentId()).isEqualTo(payment.getId());
        assertThat(response.getStudentId()).isEqualTo(payment.getStudentId());
        assertThat(response.getCourseId()).isEqualTo(payment.getCourseId());
        assertThat(response.getStripePaymentIntentId()).isEqualTo("pi_admin_detail");
        assertThat(response.toString())
                .doesNotContain("secret_detail")
                .doesNotContain("operation-detail");
    }

    @Test
    void getAdminPayment_whenPaymentIsMissing_throwsNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getAdminPayment(paymentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void verifySuccessfulPayment_whenSuccessExists_returnsTrue() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(currentUserService.hasAnyRole("STUDENT")).thenReturn(false);
        when(paymentRepository.existsByStudentIdAndCourseIdAndStatus(studentId, courseId, PaymentStatus.SUCCESS))
                .thenReturn(true);

        PaymentVerificationResponse response = paymentService.verifySuccessfulPayment(studentId, courseId);

        assertThat(response.getStudentId()).isEqualTo(studentId);
        assertThat(response.getCourseId()).isEqualTo(courseId);
        assertThat(response.isSuccessfulPaymentExists()).isTrue();
    }

    @Test
    void verifySuccessfulPayment_whenNoSuccessExists_returnsFalse() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(currentUserService.hasAnyRole("STUDENT")).thenReturn(false);
        when(paymentRepository.existsByStudentIdAndCourseIdAndStatus(studentId, courseId, PaymentStatus.SUCCESS))
                .thenReturn(false);

        PaymentVerificationResponse response = paymentService.verifySuccessfulPayment(studentId, courseId);

        assertThat(response.isSuccessfulPaymentExists()).isFalse();
    }

    @Test
    void verifySuccessfulPayment_whenStudentVerifiesAnotherStudent_throwsAccessDenied() {
        UUID authenticatedStudentId = UUID.randomUUID();
        UUID requestedStudentId = UUID.randomUUID();
        when(currentUserService.hasAnyRole("STUDENT")).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(authenticatedStudentId);

        assertThatThrownBy(() -> paymentService.verifySuccessfulPayment(requestedStudentId, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Students can only verify their own payments");

        verify(paymentRepository, never()).existsByStudentIdAndCourseIdAndStatus(any(), any(), any());
    }

    @Test
    void refundPayment_whenPaymentIsSuccessful_createsStripeRefundAndMarksRefunded() {
        Payment payment = successfulPayment("pi_refundable");
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(stripeRefundGateway.createRefund(any(CreateStripeRefundRequest.class)))
                .thenReturn(new StripeRefund("re_123", "succeeded"));

        RefundPaymentResponse response = paymentService.refundPayment(
                payment.getId(),
                RefundPaymentRequest.builder().reason("Student changed schedule").build()
        );

        assertThat(response.getPaymentId()).isEqualTo(payment.getId());
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.getStripeRefundId()).isEqualTo("re_123");
        assertThat(response.getRefundedAt()).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getStripeRefundId()).isEqualTo("re_123");
        assertThat(payment.getRefundReason()).isEqualTo("Student changed schedule");
        assertThat(payment.getRefundedAt()).isNotNull();

        ArgumentCaptor<CreateStripeRefundRequest> refundRequestCaptor =
                ArgumentCaptor.forClass(CreateStripeRefundRequest.class);
        verify(stripeRefundGateway).createRefund(refundRequestCaptor.capture());
        CreateStripeRefundRequest refundRequest = refundRequestCaptor.getValue();
        assertThat(refundRequest.paymentIntentId()).isEqualTo("pi_refundable");
        assertThat(refundRequest.paymentId()).isEqualTo(payment.getId());
        assertThat(refundRequest.idempotencyKey()).isEqualTo("refund:" + payment.getId());
        assertThat(refundRequest.reason()).isEqualTo("Student changed schedule");
        verify(paymentRepository).saveAndFlush(payment);
    }

    @Test
    void refundPayment_whenPaymentIsPending_rejectsWithoutCallingStripe() {
        Payment payment = pendingPayment("pi_pending_refund");
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refundPayment(payment.getId(), RefundPaymentRequest.builder().build()))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessage("Only successful payments can be refunded");

        verify(stripeRefundGateway, never()).createRefund(any(CreateStripeRefundRequest.class));
        verify(paymentRepository, never()).saveAndFlush(payment);
    }

    @Test
    void refundPayment_whenPaymentIsFailed_rejectsWithoutCallingStripe() {
        Payment payment = pendingPayment("pi_failed_refund");
        payment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refundPayment(payment.getId(), null))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessage("Only successful payments can be refunded");

        verify(stripeRefundGateway, never()).createRefund(any(CreateStripeRefundRequest.class));
        verify(paymentRepository, never()).saveAndFlush(payment);
    }

    @Test
    void refundPayment_whenAlreadyRefunded_rejectsDuplicateRefund() {
        Payment payment = successfulPayment("pi_already_refunded");
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setStripeRefundId("re_existing");
        payment.setRefundedAt(LocalDateTime.of(2026, 10, 10, 14, 0));
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refundPayment(payment.getId(), null))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessage("Payment has already been refunded");

        verify(stripeRefundGateway, never()).createRefund(any(CreateStripeRefundRequest.class));
        verify(paymentRepository, never()).saveAndFlush(payment);
    }

    @Test
    void refundPayment_whenStripeFails_doesNotMarkPaymentRefunded() {
        Payment payment = successfulPayment("pi_refund_failure");
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(stripeRefundGateway.createRefund(any(CreateStripeRefundRequest.class)))
                .thenThrow(new PaymentProviderException("Stripe refund creation failed"));

        assertThatThrownBy(() -> paymentService.refundPayment(payment.getId(), null))
                .isInstanceOf(PaymentProviderException.class);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getStripeRefundId()).isNull();
        assertThat(payment.getRefundedAt()).isNull();
        verify(paymentRepository, never()).saveAndFlush(payment);
    }

    @Test
    void refundPayment_whenStripeRefundIsNotSucceeded_doesNotMarkPaymentRefunded() {
        Payment payment = successfulPayment("pi_refund_pending");
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(stripeRefundGateway.createRefund(any(CreateStripeRefundRequest.class)))
                .thenReturn(new StripeRefund("re_pending", "pending"));

        assertThatThrownBy(() -> paymentService.refundPayment(payment.getId(), null))
                .isInstanceOf(PaymentProviderException.class)
                .hasMessage("Stripe refund was not confirmed as successful");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getStripeRefundId()).isNull();
        assertThat(payment.getRefundedAt()).isNull();
        verify(paymentRepository, never()).saveAndFlush(payment);
    }

    @Test
    void handleStripeWebhook_whenPaymentIntentSucceeded_marksPaymentSuccessfulAndStoresPaidAt() {
        String stripePaymentIntentId = "pi_success_001";
        LocalDateTime paidAt = LocalDateTime.of(2026, 10, 10, 8, 30);
        Payment payment = pendingPayment(stripePaymentIntentId);
        when(stripeWebhookVerifier.verify("payload", "signature"))
                .thenReturn(new StripeWebhookEvent("evt_1", "payment_intent.succeeded", stripePaymentIntentId, payment.getId(), paidAt));
        when(paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)).thenReturn(java.util.Optional.of(payment));

        paymentService.handleStripeWebhook("payload", "signature");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
        verify(paymentRepository).saveAndFlush(payment);
    }

    @Test
    void handleStripeWebhook_whenPaymentIntentFailed_marksPendingPaymentFailed() {
        String stripePaymentIntentId = "pi_failed_001";
        Payment payment = pendingPayment(stripePaymentIntentId);
        when(stripeWebhookVerifier.verify("payload", "signature"))
                .thenReturn(new StripeWebhookEvent("evt_2", "payment_intent.payment_failed", stripePaymentIntentId, payment.getId(), LocalDateTime.now()));
        when(paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)).thenReturn(java.util.Optional.of(payment));

        paymentService.handleStripeWebhook("payload", "signature");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPaidAt()).isNull();
        verify(paymentRepository).saveAndFlush(payment);
    }

    @Test
    void handleStripeWebhook_whenPaymentIntentIsUnknown_doesNotMutatePayments() {
        String stripePaymentIntentId = "pi_unknown_001";
        when(stripeWebhookVerifier.verify("payload", "signature"))
                .thenReturn(new StripeWebhookEvent("evt_3", "payment_intent.succeeded", stripePaymentIntentId, UUID.randomUUID(), LocalDateTime.now()));
        when(paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)).thenReturn(java.util.Optional.empty());

        paymentService.handleStripeWebhook("payload", "signature");

        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
    }

    @Test
    void handleStripeWebhook_whenEventWasAlreadyProcessed_doesNotProcessAgain() {
        when(stripeWebhookVerifier.verify("payload", "signature"))
                .thenReturn(new StripeWebhookEvent(
                        "evt_duplicate",
                        "payment_intent.succeeded",
                        "pi_duplicate",
                        UUID.randomUUID(),
                        LocalDateTime.now()
                ));
        when(processedStripeWebhookEventRepository.insertIfAbsent(any(), any(), any(), any())).thenReturn(0);

        paymentService.handleStripeWebhook("payload", "signature");

        verify(paymentRepository, never()).findByStripePaymentIntentId(any());
        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
    }

    @Test
    void handleStripeWebhook_whenSuccessEventIsDeliveredAgain_doesNotSaveAgain() {
        String stripePaymentIntentId = "pi_success_duplicate";
        Payment payment = pendingPayment(stripePaymentIntentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.of(2026, 10, 10, 8, 30));
        when(stripeWebhookVerifier.verify("payload", "signature"))
                .thenReturn(new StripeWebhookEvent("evt_4", "payment_intent.succeeded", stripePaymentIntentId, payment.getId(), LocalDateTime.now()));
        when(paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)).thenReturn(java.util.Optional.of(payment));

        paymentService.handleStripeWebhook("payload", "signature");

        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
    }

    @Test
    void handleStripeWebhook_whenSuccessArrivesAfterFailure_rejectsInvalidTransition() {
        String stripePaymentIntentId = "pi_failed_then_success";
        Payment payment = pendingPayment(stripePaymentIntentId);
        payment.setStatus(PaymentStatus.FAILED);
        when(stripeWebhookVerifier.verify("payload", "signature"))
                .thenReturn(new StripeWebhookEvent("evt_invalid_transition", "payment_intent.succeeded",
                        stripePaymentIntentId, payment.getId(), LocalDateTime.now()));
        when(paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)).thenReturn(Optional.of(payment));

        paymentService.handleStripeWebhook("payload", "signature");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPaidAt()).isNull();
        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
    }

    @Test
    void handleStripeWebhook_whenPaymentMetadataDoesNotMatchRecord_rejectsReferenceSafely() {
        String stripePaymentIntentId = "pi_reference_mismatch";
        Payment payment = pendingPayment(stripePaymentIntentId);
        when(stripeWebhookVerifier.verify("payload", "signature"))
                .thenReturn(new StripeWebhookEvent("evt_reference_mismatch", "payment_intent.succeeded",
                        stripePaymentIntentId, UUID.randomUUID(), LocalDateTime.now()));
        when(paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)).thenReturn(Optional.of(payment));

        paymentService.handleStripeWebhook("payload", "signature");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
    }

    @Test
    void handleStripeWebhook_whenVerifierRejectsSignature_propagatesControlledExceptionWithoutMutation() {
        when(stripeWebhookVerifier.verify("payload", "bad-signature"))
                .thenThrow(new PaymentDomainException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature"));

        assertThatThrownBy(() -> paymentService.handleStripeWebhook("payload", "bad-signature"))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessage("Invalid Stripe webhook signature");

        verify(paymentRepository, never()).findByStripePaymentIntentId(any());
        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
        verifyNoInteractions(processedStripeWebhookEventRepository);
    }

    private CourseLookupResponse paidCourse(UUID courseId, String price) {
        return new CourseLookupResponse(
                courseId,
                "Physics",
                new BigDecimal(price),
                true,
                true
        );
    }

    private Payment pendingPayment(String stripePaymentIntentId) {
        Payment payment = Payment.builder()
                .studentId(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("LKR")
                .status(PaymentStatus.PENDING)
                .stripePaymentIntentId(stripePaymentIntentId)
                .build();
        payment.setId(UUID.randomUUID());
        return payment;
    }

    private Payment successfulPayment(String stripePaymentIntentId) {
        Payment payment = pendingPayment(stripePaymentIntentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.of(2026, 10, 10, 12, 0));
        return payment;
    }
}
