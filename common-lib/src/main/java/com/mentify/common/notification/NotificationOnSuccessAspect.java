package com.mentify.common.notification;

import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mentify.notifications.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@Profile("!test")
public class NotificationOnSuccessAspect {

    private final NotificationEventPublisher notificationEventPublisher;

    @Around("@annotation(notification)")
    public Object publishAfterSuccessfulResponse(
            ProceedingJoinPoint joinPoint,
            NotifyOnSuccess notification
    ) throws Throwable {
        Object result = joinPoint.proceed();
        if (isSuccessful(result)) {
            notificationEventPublisher.publishForCurrentUser(
                    notification.type(),
                    notification.title(),
                    notification.message()
            );
        }
        return result;
    }

    private boolean isSuccessful(Object result) {
        if (result instanceof ResponseEntity<?> response && !response.getStatusCode().is2xxSuccessful()) {
            return false;
        }
        if (result instanceof ApiResponse<?> response
                && response.getStatusCode() != null
                && (response.getStatusCode() < 200 || response.getStatusCode() >= 300)) {
            return false;
        }
        return true;
    }
}
