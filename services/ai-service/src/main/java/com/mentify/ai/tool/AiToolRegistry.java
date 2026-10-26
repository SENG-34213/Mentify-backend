package com.mentify.ai.tool;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Central inventory of tools exposed to the admin assistant.
 *
 * Keeping this inventory separate from the provider prompt prevents the model
 * from discovering arbitrary service endpoints and gives the admin UI and
 * observability layer a single source of truth.
 */
@Component
public class AiToolRegistry {

    private static final String ADMIN = "ROLE_ADMIN";
    private static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    private final Map<String, AiToolDescriptor> tools;

    public AiToolRegistry(List<AiTool<?>> implementedTools) {
        this.tools = implementedTools.stream()
                .map(tool -> descriptor(tool.getName()))
                .collect(Collectors.toUnmodifiableMap(
                        AiToolDescriptor::name,
                        Function.identity(),
                        (first, ignored) -> first));
    }

    public List<AiToolDescriptor> list() {
        return tools.values().stream()
                .filter(this::isAdminTool)
                .sorted(Comparator.comparing(AiToolDescriptor::name))
                .toList();
    }

    public AiToolDescriptor get(String name) {
        return tools.get(name);
    }

    public boolean contains(String name) {
        return tools.containsKey(name);
    }

    /**
     * Resolves only explicitly registered admin intents. Complex legacy
     * intents such as course/date disambiguation remain handled by their
     * specialized routing code.
     */
    public Optional<AiToolDescriptor> resolveAdminIntent(String message) {
        String normalized = message == null ? "" : message.toLowerCase(java.util.Locale.ROOT);
        return tools.values().stream()
                .filter(tool -> tool.allowedRoles().contains(ADMIN) || tool.allowedRoles().contains(SUPER_ADMIN))
                .filter(tool -> tool.triggerPhrases().stream().anyMatch(normalized::contains))
                .sorted(Comparator.comparingInt(this::longestTriggerPhrase).reversed())
                .findFirst();
    }

    private int longestTriggerPhrase(AiToolDescriptor descriptor) {
        return descriptor.triggerPhrases().stream().mapToInt(String::length).max().orElse(0);
    }

    private AiToolDescriptor descriptor(String name) {
        Objects.requireNonNull(name, "AI tool name must not be null");
        return switch (name) {
            case "getAdminDashboardOverview" -> new AiToolDescriptor(
                    name, "Aggregates admin KPIs across users, courses, enrollments, payments, attendance, assignments, quizzes, and exams.", "analytics-service", roles(), true,
                    Set.of("admin dashboard", "system overview", "overall statistics", "overall metrics", "all services", "platform overview"));
            case "getAiUsageOverview" -> new AiToolDescriptor(
                    name, "Summarizes AI requests, failures, tokens, cost, and latency for the last 30 days.", "ai-service", roles(), true,
                    Set.of("ai usage", "chatbot usage", "ai token", "ai cost", "ai latency", "failed ai request"));
            case "getMaterialOverview" -> new AiToolDescriptor(
                    name, "Summarizes the bounded, paginated material inventory visible to the admin.", "material-service", roles(), true,
                    Set.of("material", "learning file", "uploaded file", "study material"));
            case "getNotificationOverview" -> new AiToolDescriptor(
                    name, "Summarizes notification volume and email delivery status.", "notification-service", roles(), true,
                    Set.of("notification", "email delivery", "failed email", "unread notification"));
            case "getCommunicationOverview" -> new AiToolDescriptor(
                    name, "Summarizes communication groups and message status without exposing message content.", "communication-service", roles(), true,
                    Set.of("communication", "message activity", "communication group", "messaging overview"));
            case "getSystemHealthOverview" -> new AiToolDescriptor(
                    name, "Checks discovered service actuator health with bounded timeouts.", "service-discovery", roles(), true,
                    Set.of("system health", "service health", "health check", "service status", "all services healthy"));
            case "getUserRegistrationOverview" -> new AiToolDescriptor(
                    name, "Summarizes registered students and teachers.", "user-service", roles(), true);
            case "getAssignmentPerformance" -> new AiToolDescriptor(
                    name, "Summarizes assignment completion and marks.", "assignment-service", roles(), true);
            case "getAttendancePerformance" -> new AiToolDescriptor(
                    name, "Summarizes attendance for an authorized scope.", "attendance-service", roles(), true);
            case "getQuizPerformance" -> new AiToolDescriptor(
                    name, "Summarizes quiz performance for an authorized scope.", "analytics-service", roles(), true);
            case "getTodayQuizPerformance" -> new AiToolDescriptor(
                    name, "Summarizes quizzes scheduled for today.", "analytics-service", roles(), true);
            case "getUpcomingCalendarEvents" -> new AiToolDescriptor(
                    name, "Lists upcoming calendar events visible to the admin.", "calender-service", roles(), true);
            case "getStudentProgress" -> new AiToolDescriptor(
                    name, "Reads the authenticated student's learning progress.", "student-data", Set.of("ROLE_STUDENT"), true);
            default -> new AiToolDescriptor(
                    name, "Registered Mentify AI read-only tool.", "unknown", roles(), true);
        };
    }

    private Set<String> roles() {
        return Set.of(ADMIN, SUPER_ADMIN);
    }

    private boolean isAdminTool(AiToolDescriptor tool) {
        return tool.allowedRoles().contains(ADMIN) || tool.allowedRoles().contains(SUPER_ADMIN);
    }
}
