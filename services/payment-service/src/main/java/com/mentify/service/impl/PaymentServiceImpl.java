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
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final CourseServiceClient courseServiceClient;
    private final AuthenticatedUserService currentUserService;
    private final StripePaymentIntentGateway stripePaymentIntentGateway;
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
