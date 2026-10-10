package com.mentify.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.client.CourseServiceClient;
import com.mentify.client.dto.CourseLookupResponse;
import com.mentify.config.StripeProperties;
import com.mentify.dto.StartCoursePaymentRequest;
import com.mentify.dto.StartCoursePaymentResponse;
import com.mentify.entity.Payment;
import com.mentify.enums.PaymentStatus;
import com.mentify.exception.PaymentDomainException;
import com.mentify.exception.PaymentProviderException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.PaymentRepository;
import com.mentify.security.AuthenticatedUserService;
import com.mentify.service.stripe.CreateStripePaymentIntentRequest;
import com.mentify.service.stripe.StripePaymentIntent;
import com.mentify.service.stripe.StripePaymentIntentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    private static final String AUTHORIZATION_HEADER = "Bearer student-token";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private AuthenticatedUserService currentUserService;

    @Mock
    private StripePaymentIntentGateway stripePaymentIntentGateway;

    private StripeProperties stripeProperties;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        stripeProperties = new StripeProperties();
        stripeProperties.setCurrency("LKR");
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                courseServiceClient,
                currentUserService,
                stripePaymentIntentGateway,
                stripeProperties
        );

        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(UUID.randomUUID());
            }
            return payment;
        });
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

        ArgumentCaptor<CreateStripePaymentIntentRequest> stripeRequestCaptor =
                ArgumentCaptor.forClass(CreateStripePaymentIntentRequest.class);
        verify(stripePaymentIntentGateway).createPaymentIntent(stripeRequestCaptor.capture());
        CreateStripePaymentIntentRequest stripeRequest = stripeRequestCaptor.getValue();
        assertThat(stripeRequest.amountInMinorUnits()).isEqualTo(149999L);
        assertThat(stripeRequest.currency()).isEqualTo("LKR");
        assertThat(stripeRequest.studentId()).isEqualTo(studentId);
        assertThat(stripeRequest.courseId()).isEqualTo(courseId);
        assertThat(stripeRequest.paymentId()).isEqualTo(finalPayment.getId());
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

    private CourseLookupResponse paidCourse(UUID courseId, String price) {
        return new CourseLookupResponse(
                courseId,
                "Physics",
                new BigDecimal(price),
                true,
                true
        );
    }
}
