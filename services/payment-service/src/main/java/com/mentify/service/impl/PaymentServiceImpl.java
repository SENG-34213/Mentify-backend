package com.mentify.service.impl;

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
import com.mentify.repository.PaymentSpecifications;
import com.mentify.repository.ProcessedStripeWebhookEventRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded";
    private static final String PAYMENT_INTENT_PAYMENT_FAILED = "payment_intent.payment_failed";
    private static final int DEFAULT_ADMIN_PAGE_SIZE = 20;

    private final PaymentRepository paymentRepository;
    private final ProcessedStripeWebhookEventRepository processedStripeWebhookEventRepository;
    private final CourseServiceClient courseServiceClient;
    private final AuthenticatedUserService currentUserService;
    private final StripePaymentIntentGateway stripePaymentIntentGateway;
    private final StripeWebhookVerifier stripeWebhookVerifier;
    private final StripeProperties stripeProperties;

    @Override
    @Transactional
    public StartCoursePaymentResponse startCoursePayment(StartCoursePaymentRequest request, String authorizationHeader) {
        UUID studentId = currentUserService.getCurrentUserId();
        CourseLookupResponse course = fetchCourse(request.getCourseId(), authorizationHeader);
        validatePayableCourse(course);

        String currency = resolveCurrency();
        BigDecimal amount = resolvePaymentAmount(course);
        String paymentOperationKey = paymentOperationKey(studentId, course.getId(), currency, amount);

        Payment payment = paymentRepository
                .findFirstByPaymentOperationKeyAndStatusOrderByCreatedAtDesc(paymentOperationKey, PaymentStatus.PENDING)
                .orElseGet(() -> paymentRepository.saveAndFlush(Payment.builder()
                        .studentId(studentId)
                        .courseId(course.getId())
                        .amount(amount)
                        .currency(currency)
                        .status(PaymentStatus.PENDING)
                        .provider(PaymentProvider.STRIPE)
                        .paymentOperationKey(paymentOperationKey)
                        .build()));

        if (hasReusableStripeIntent(payment)) {
            return toStartCoursePaymentResponse(payment);
        }

        try {
            StripePaymentIntent paymentIntent = stripePaymentIntentGateway.createPaymentIntent(
                    new CreateStripePaymentIntentRequest(
                            StripeAmountConverter.toMinorUnits(amount, currency),
                            currency,
                            payment.getId(),
                            studentId,
                            course.getId(),
                            stripeIdempotencyKey(payment.getId())
                    )
            );

            validateStripePaymentIntentReference(payment, paymentIntent.id());
            payment.setStripePaymentIntentId(paymentIntent.id());
            payment.setStripeClientSecret(paymentIntent.clientSecret());
            Payment savedPayment = paymentRepository.saveAndFlush(payment);

            return toStartCoursePaymentResponse(savedPayment);
        } catch (PaymentProviderException ex) {
            transitionPaymentStatus(payment, PaymentStatus.FAILED, null);
            paymentRepository.saveAndFlush(payment);
            throw ex;
        } catch (ArithmeticException ex) {
            transitionPaymentStatus(payment, PaymentStatus.FAILED, null);
            paymentRepository.saveAndFlush(payment);
            throw new PaymentDomainException(HttpStatus.BAD_REQUEST, "Course price cannot be converted to the configured payment currency");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentSummaryResponse> getCurrentStudentPayments() {
        UUID studentId = currentUserService.getCurrentUserId();
        return paymentRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(this::toPaymentSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailResponse getCurrentStudentPayment(UUID paymentId) {
        UUID studentId = currentUserService.getCurrentUserId();
        Payment payment = findOwnedPayment(paymentId, studentId);
        return toPaymentDetailResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getCurrentStudentPaymentStatus(UUID paymentId) {
        UUID studentId = currentUserService.getCurrentUserId();
        Payment payment = findOwnedPayment(paymentId, studentId);
        return toPaymentStatusResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPaymentSummaryResponse> getAdminPayments(AdminPaymentFilter filter, Pageable pageable) {
        validateAdminPaymentFilter(filter);
        return paymentRepository.findAll(PaymentSpecifications.adminFilter(filter), recentFirst(pageable))
                .map(this::toAdminPaymentSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPaymentDetailResponse getAdminPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        return toAdminPaymentDetailResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentVerificationResponse verifySuccessfulPayment(UUID studentId, UUID courseId) {
        if (currentUserService.hasAnyRole("STUDENT") && !currentUserService.getCurrentUserId().equals(studentId)) {
            throw new AccessDeniedException("Students can only verify their own payments");
        }

        boolean successfulPaymentExists = paymentRepository.existsByStudentIdAndCourseIdAndStatus(
                studentId,
                courseId,
                PaymentStatus.SUCCESS
        );

        return PaymentVerificationResponse.builder()
                .studentId(studentId)
                .courseId(courseId)
                .successfulPaymentExists(successfulPaymentExists)
                .build();
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String signatureHeader) {
        StripeWebhookEvent event = stripeWebhookVerifier.verify(payload, signatureHeader);
        if (!claimWebhookEvent(event)) {
            log.debug("Ignoring duplicate Stripe webhook event [{}]", event.eventId());
            return;
        }

        if (PAYMENT_INTENT_SUCCEEDED.equals(event.type())) {
            markPaymentSuccessful(event);
            return;
        }

        if (PAYMENT_INTENT_PAYMENT_FAILED.equals(event.type())) {
            markPaymentFailed(event);
            return;
        }

        log.debug("Ignoring unsupported Stripe webhook event type [{}]", event.type());
    }

    private void markPaymentSuccessful(StripeWebhookEvent event) {
        paymentRepository.findByStripePaymentIntentId(event.paymentIntentId())
                .ifPresentOrElse(payment -> {
                    if (!matchesWebhookPaymentReference(payment, event)) {
                        return;
                    }
                    if (!transitionPaymentStatus(payment, PaymentStatus.SUCCESS, event.createdAt())) {
                        log.warn("Ignoring invalid success transition for payment [{}] currently [{}]", payment.getId(), payment.getStatus());
                        return;
                    }
                    paymentRepository.saveAndFlush(payment);
                }, () -> log.warn("Stripe webhook referenced unknown PaymentIntent [{}]", event.paymentIntentId()));
    }

    private void markPaymentFailed(StripeWebhookEvent event) {
        paymentRepository.findByStripePaymentIntentId(event.paymentIntentId())
                .ifPresentOrElse(payment -> {
                    if (!matchesWebhookPaymentReference(payment, event)) {
                        return;
                    }
                    if (!transitionPaymentStatus(payment, PaymentStatus.FAILED, null)) {
                        log.warn("Ignoring invalid failure transition for payment [{}] currently [{}]", payment.getId(), payment.getStatus());
                        return;
                    }
                    paymentRepository.saveAndFlush(payment);
                }, () -> log.warn("Stripe webhook referenced unknown PaymentIntent [{}]", event.paymentIntentId()));
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

    private BigDecimal resolvePaymentAmount(CourseLookupResponse course) {
        try {
            return course.getCourseFeeMonthly().setScale(2);
        } catch (ArithmeticException ex) {
            throw new PaymentDomainException(HttpStatus.BAD_REQUEST, "Course price cannot be converted to the configured payment currency");
        }
    }

    private boolean claimWebhookEvent(StripeWebhookEvent event) {
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new PaymentDomainException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook payload");
        }

        return processedStripeWebhookEventRepository.insertIfAbsent(
                event.eventId(),
                event.type(),
                event.paymentIntentId(),
                LocalDateTime.now()
        ) == 1;
    }

    private Payment findOwnedPayment(UUID paymentId, UUID studentId) {
        return paymentRepository.findByIdAndStudentId(paymentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
    }

    private void validateAdminPaymentFilter(AdminPaymentFilter filter) {
        if (filter != null
                && filter.getCreatedFrom() != null
                && filter.getCreatedTo() != null
                && filter.getCreatedFrom().isAfter(filter.getCreatedTo())) {
            throw new PaymentDomainException(HttpStatus.BAD_REQUEST, "createdFrom must be before or equal to createdTo");
        }
    }

    private Pageable recentFirst(Pageable pageable) {
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, DEFAULT_ADMIN_PAGE_SIZE, sort);
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private boolean matchesWebhookPaymentReference(Payment payment, StripeWebhookEvent event) {
        if (event.paymentId() != null && !event.paymentId().equals(payment.getId())) {
            log.warn("Stripe webhook event [{}] payment_id metadata did not match payment record [{}]",
                    event.eventId(), payment.getId());
            return false;
        }

        if (!event.paymentIntentId().equals(payment.getStripePaymentIntentId())) {
            log.warn("Stripe webhook event [{}] PaymentIntent reference did not match payment record [{}]",
                    event.eventId(), payment.getId());
            return false;
        }

        return true;
    }

    private boolean transitionPaymentStatus(Payment payment, PaymentStatus targetStatus, LocalDateTime paidAt) {
        PaymentStatus currentStatus = payment.getStatus();
        if (currentStatus == targetStatus) {
            return false;
        }

        if (currentStatus != PaymentStatus.PENDING) {
            return false;
        }

        if (targetStatus != PaymentStatus.SUCCESS && targetStatus != PaymentStatus.FAILED) {
            return false;
        }

        payment.setStatus(targetStatus);
        if (targetStatus == PaymentStatus.SUCCESS) {
            payment.setPaidAt(paidAt);
        }
        return true;
    }

    private void validateStripePaymentIntentReference(Payment payment, String stripePaymentIntentId) {
        paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .filter(existingPayment -> !existingPayment.getId().equals(payment.getId()))
                .ifPresent(existingPayment -> {
                    throw new PaymentDomainException(HttpStatus.CONFLICT, "Stripe payment reference is already linked to another payment");
                });
    }

    private boolean hasReusableStripeIntent(Payment payment) {
        return payment.getStripePaymentIntentId() != null
                && !payment.getStripePaymentIntentId().isBlank()
                && payment.getStripeClientSecret() != null
                && !payment.getStripeClientSecret().isBlank();
    }

    private StartCoursePaymentResponse toStartCoursePaymentResponse(Payment payment) {
        return StartCoursePaymentResponse.builder()
                .paymentId(payment.getId())
                .courseId(payment.getCourseId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .clientSecret(payment.getStripeClientSecret())
                .build();
    }

    private PaymentSummaryResponse toPaymentSummaryResponse(Payment payment) {
        return PaymentSummaryResponse.builder()
                .paymentId(payment.getId())
                .courseId(payment.getCourseId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private PaymentDetailResponse toPaymentDetailResponse(Payment payment) {
        return PaymentDetailResponse.builder()
                .paymentId(payment.getId())
                .courseId(payment.getCourseId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .provider(payment.getProvider())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private PaymentStatusResponse toPaymentStatusResponse(Payment payment) {
        return PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private AdminPaymentSummaryResponse toAdminPaymentSummaryResponse(Payment payment) {
        return AdminPaymentSummaryResponse.builder()
                .paymentId(payment.getId())
                .studentId(payment.getStudentId())
                .courseId(payment.getCourseId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private AdminPaymentDetailResponse toAdminPaymentDetailResponse(Payment payment) {
        return AdminPaymentDetailResponse.builder()
                .paymentId(payment.getId())
                .studentId(payment.getStudentId())
                .courseId(payment.getCourseId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .provider(payment.getProvider())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private String paymentOperationKey(UUID studentId, UUID courseId, String currency, BigDecimal amount) {
        return studentId + "|" + courseId + "|" + currency + "|" + amount.setScale(2).toPlainString();
    }

    private String stripeIdempotencyKey(UUID paymentId) {
        return "payment-intent:" + paymentId;
    }
}
