package com.mentify.common.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mentify.notifications.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@Profile("!test")
public class NotificationOnSuccessAspect {

    private final NotificationEventPublisher notificationEventPublisher;
    private final ObjectMapper objectMapper;

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
                    messageWithDetails(result, notification.message())
            );
        }
        return result;
    }

    private String messageWithDetails(Object result, String message) {
        Object payload = result instanceof ResponseEntity<?> response ? response.getBody() : result;
        if (payload instanceof ApiResponse<?> response) {
            payload = response.getData();
        }
        if (payload == null) {
            return message;
        }

        JsonNode data;
        try {
            data = objectMapper.valueToTree(payload);
        } catch (RuntimeException ignored) {
            return message;
        }
        if (!data.isObject()) {
            return message;
        }

        List<String> details = new ArrayList<>();
        addDetail(data, details, "title", "Title");
        addDetail(data, details, "name", "Name");
        addDetail(data, details, "groupName", "Group");
        addDetail(data, details, "courseName", "Course");
        addDetail(data, details, "courseCode", "Course code");
        addDetail(data, details, "examDate", "Exam date");
        addDetail(data, details, "attendanceDate", "Date");
        addDetail(data, details, "startDateTime", "Starts");
        addDetail(data, details, "startDate", "Starts");
        addDetail(data, details, "dueDate", "Due");
        addDetail(data, details, "startTime", "Start time");
        addDetail(data, details, "endTime", "End time");
        addDetail(data, details, "submittedAt", "Submitted");
        addDetail(data, details, "gradedAt", "Graded");
        addDetail(data, details, "returnedAt", "Returned");
        addDetail(data, details, "publishedAt", "Published");
        addDetail(data, details, "markedAt", "Marked");
        addDetail(data, details, "venue", "Venue");
        addDetail(data, details, "location", "Location");
        addDetail(data, details, "mode", "Mode");
        addDetail(data, details, "durationMinutes", "Duration (minutes)");
        addDetail(data, details, "duration", "Duration");
        addDetail(data, details, "maxAttempts", "Allowed attempts");
        addDetail(data, details, "attemptNumber", "Attempt");
        addDetail(data, details, "totalMarks", "Total marks");
        addDetail(data, details, "maxMarks", "Maximum marks");
        addDetail(data, details, "passMarks", "Pass marks");
        addDetail(data, details, "marksObtained", "Marks obtained");
        addDetail(data, details, "grade", "Grade");
        addDetail(data, details, "status", "Status");
        addDetail(data, details, "fileName", "File");
        addDetail(data, details, "originalFilename", "File");
        addDetail(data, details, "fileType", "File type");
        addDetail(data, details, "contentType", "File format");
        addDetail(data, details, "originalFileName", "File");
        addDetail(data, details, "fileSize", "File size");
        addDetail(data, details, "purpose", "Material purpose");
        addDetail(data, details, "studentName", "Student");
        addDetail(data, details, "memberCount", "Members");
        addDetail(data, details, "joinedAt", "Joined");
        addDetail(data, details, "amount", "Amount");
        addDetail(data, details, "currency", "Currency");
        addDetail(data, details, "paidAt", "Paid at");
        addDetail(data, details, "refundedAt", "Refunded at");
        addDetail(data, details, "refundReason", "Refund reason");
        addDetail(data, details, "description", "Description");
        addAttendanceSummary(data, details);

        return details.isEmpty() ? message : message + " Details: " + String.join("; ", details) + ".";
    }

    private void addAttendanceSummary(JsonNode data, List<String> details) {
        JsonNode records = data.get("records");
        if (records == null || !records.isArray() || records.size() == 0) {
            return;
        }
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        records.forEach(record -> {
            JsonNode status = record.get("status");
            if (status != null && !status.isNull() && StringUtils.hasText(status.asText())) {
                counts.merge(status.asText().replace('_', ' ').toLowerCase(), 1, Integer::sum);
            }
        });
        if (!counts.isEmpty()) {
            String summary = counts.entrySet().stream()
                    .map(entry -> entry.getValue() + " " + entry.getKey())
                    .collect(java.util.stream.Collectors.joining(", "));
            details.add("Attendance records: " + summary);
        }
    }

    private void addDetail(JsonNode data, List<String> details, String field, String label) {
        JsonNode value = data.get(field);
        if (value == null || value.isNull() || value.isContainerNode()) {
            return;
        }
        String text = value.asText();
        if (!StringUtils.hasText(text)) {
            return;
        }
        if (field.equals("description") && text.length() > 240) {
            text = text.substring(0, 237) + "...";
        }
        details.add(label + ": " + text);
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
