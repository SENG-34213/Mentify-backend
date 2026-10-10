package com.mentify.service.impl;

import com.mentify.client.CourseServiceClient;
import com.mentify.client.dto.CourseLookupResponse;
import com.mentify.config.StripeProperties;
import com.mentify.dto.StartCoursePaymentRequest;
import com.mentify.dto.StartCoursePaymentResponse;
import com.mentify.entity.Payment;
import com.mentify.enums.PaymentProvider;
import com.mentify.enums.PaymentStatus;
import com.mentify.exception.PaymentDomainException;
import com.mentify.exception.PaymentProviderException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.PaymentRepository;
import com.mentify.security.AuthenticatedUserService;
import com.mentify.service.PaymentService;
import com.mentify.service.stripe.CreateStripePaymentIntentRequest;
import com.mentify.service.stripe.StripePaymentIntent;
import com.mentify.service.stripe.StripePaymentIntentGateway;
import com.mentify.service.stripe.StripeWebhookEvent;
import com.mentify.service.stripe.StripeWebhookVerifier;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded";
    private static final String PAYMENT_INTENT_PAYMENT_FAILED = "payment_intent.payment_failed";

    private final PaymentRepository paymentRepository;
    private final CourseServiceClient courseServiceClient;
    private final AuthenticatedUserService currentUserService;
    private final StripePaymentIntentGateway stripePaymentIntentGateway;
    private final StripeWebhookVerifier stripeWebhookVerifier;
    private final StripeProperties stripeProperties;

    @Override
    public StartCoursePaymentResponse startCoursePayment(StartCoursePaymentRequest request, String authorizationHeader) {
        UUID studentId = currentUserService.getCurrentUserId();
        CourseLookupResponse course = fetchCourse(request.getCourseId(), authorizationHeader);
        validatePayableCourse(course);

        String currency = resolveCurrency();
        BigDecimal amount = course.getCourseFeeMonthly();

        Payment payment = paymentRepository.saveAndFlush(Payment.builder()
                .studentId(studentId)
                .courseId(course.getId())
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .provider(PaymentProvider.STRIPE)
                .build());

        try {
            StripePaymentIntent paymentIntent = stripePaymentIntentGateway.createPaymentIntent(
                    new CreateStripePaymentIntentRequest(
                            StripeAmountConverter.toMinorUnits(amount, currency),
                            currency,
                            payment.getId(),
                            studentId,
                            course.getId()
                    )
            );

            payment.setStripePaymentIntentId(paymentIntent.id());
            Payment savedPayment = paymentRepository.saveAndFlush(payment);

            return StartCoursePaymentResponse.builder()
                    .paymentId(savedPayment.getId())
                    .courseId(savedPayment.getCourseId())
                    .amount(savedPayment.getAmount())
                    .currency(savedPayment.getCurrency())
                    .status(savedPayment.getStatus())
                    .clientSecret(paymentIntent.clientSecret())
                    .build();
        } catch (PaymentProviderException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.saveAndFlush(payment);
            throw ex;
        } catch (ArithmeticException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.saveAndFlush(payment);
            throw new PaymentDomainException(HttpStatus.BAD_REQUEST, "Course price cannot be converted to the configured payment currency");
        }
    }

    @Override
    public void handleStripeWebhook(String payload, String signatureHeader) {
        StripeWebhookEvent event = stripeWebhookVerifier.verify(payload, signatureHeader);

        if (PAYMENT_INTENT_SUCCEEDED.equals(event.type())) {
            markPaymentSuccessful(event.paymentIntentId(), event.createdAt());
            return;
        }

        if (PAYMENT_INTENT_PAYMENT_FAILED.equals(event.type())) {
            markPaymentFailed(event.paymentIntentId());
            return;
        }

        log.debug("Ignoring unsupported Stripe webhook event type [{}]", event.type());
    }

    private void markPaymentSuccessful(String stripePaymentIntentId, LocalDateTime paidAt) {
        paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .ifPresentOrElse(payment -> {
                    if (payment.getStatus() == PaymentStatus.SUCCESS) {
                        return;
                    }
                    if (payment.getStatus() == PaymentStatus.REFUNDED) {
                        log.warn("Ignoring success webhook for refunded payment [{}]", payment.getId());
                        return;
                    }
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setPaidAt(paidAt);
                    paymentRepository.saveAndFlush(payment);
                }, () -> log.warn("Stripe webhook referenced unknown PaymentIntent [{}]", stripePaymentIntentId));
    }

    private void markPaymentFailed(String stripePaymentIntentId) {
        paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .ifPresentOrElse(payment -> {
                    if (payment.getStatus() == PaymentStatus.FAILED) {
                        return;
                    }
                    if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.REFUNDED) {
                        log.warn("Ignoring failure webhook for finalized payment [{}]", payment.getId());
                        return;
                    }
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.saveAndFlush(payment);
                }, () -> log.warn("Stripe webhook referenced unknown PaymentIntent [{}]", stripePaymentIntentId));
    }

    private CourseLookupResponse fetchCourse(UUID courseId, String authorizationHeader) {
        try {
            ApiResponse<CourseLookupResponse> response = courseServiceClient.getCourseById(courseId, authorizationHeader);
            CourseLookupResponse course = response.getData();
            if (course == null || course.getId() == null) {
                throw new ResourceNotFoundException("Course", "id", courseId);
            }
            return course;
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Course", "id", courseId);
        } catch (FeignException.Forbidden ex) {
            throw new PaymentDomainException(HttpStatus.FORBIDDEN, "Course validation is not allowed for this user");
        } catch (FeignException ex) {
            throw new PaymentDomainException(HttpStatus.SERVICE_UNAVAILABLE, "Course validation is currently unavailable. Please try again later.");
        }
    }

    private void validatePayableCourse(CourseLookupResponse course) {
        if (!course.isPublished() || !course.isVisible()) {
            throw new PaymentDomainException(HttpStatus.BAD_REQUEST, "Course is not available for payment");
        }

        if (course.getCourseFeeMonthly() == null || course.getCourseFeeMonthly().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentDomainException(HttpStatus.BAD_REQUEST, "Course does not require payment");
        }
    }

    private String resolveCurrency() {
        String currency = stripeProperties.getCurrency();
        if (currency == null || currency.isBlank()) {
            throw new PaymentDomainException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment currency is not configured");
        }
        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);
        if (normalizedCurrency.length() != 3) {
            throw new PaymentDomainException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment currency configuration is invalid");
        }
        return normalizedCurrency;
    }
}
