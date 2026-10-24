package com.mentify.notification.service;

import java.util.Locale;
import java.util.Set;

/**
 * Emails are reserved for messages that are actionable or carry an important account result.
 * All notification events are still stored for the in-app notification center.
 */
final class NotificationEmailPolicy {

    private static final Set<String> EMAIL_WORTHY_TYPES = Set.of(
            "ASSIGNMENT_CLOSED",
            "ASSIGNMENT_PUBLISHED",
            "COURSE_PAYMENT_INITIATED",
            "EXAM_CANCELLED",
            "EXAM_PUBLISHED",
            "EXAM_RESULTS_RECORDED",
            "EXAM_UPDATED",
            "PAYMENT_REFUNDED",
            "PAYMENT_SUCCEEDED",
            "QUIZ_CREATED",
            "SUBMISSION_RETURNED",
            "SUBMISSION_SUBMITTED"
    );

    private NotificationEmailPolicy() {
    }

    static boolean allowsEmail(String type) {
        return type != null && EMAIL_WORTHY_TYPES.contains(type.trim().toUpperCase(Locale.ROOT));
    }
}
