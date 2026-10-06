package com.mentify.exam.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.mentify.exam.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Test-only endpoints used to exercise security and error handling of the foundation. */
@RestController
@RequestMapping("/api/v1/exams/probe")
public class SecurityProbeController {

    public record ProbeRequest(@NotBlank(message = "name must not be blank") String name) {
    }

    private final CurrentUserService currentUserService;

    public SecurityProbeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/whoami")
    public Map<String, Object> whoami() {
        return Map.of(
                "userId", currentUserService.getCurrentUserId().toString(),
                "roles", currentUserService.getCurrentRoles().stream().map(Enum::name).sorted().toList());
    }

    @GetMapping("/boom")
    public String boom() {
        throw new IllegalStateException(
                "jdbc:postgresql://db:5432/exam_db?user=postgres&password=super-secret at org.hibernate.Foo");
    }

    @GetMapping("/duplicate")
    public String duplicate() {
        throw new org.springframework.dao.DataIntegrityViolationException(
                "duplicate key value violates unique constraint uk_exam_student jdbc:postgresql://db/exam");
    }

    @GetMapping("/dependency")
    public String dependency() {
        feign.Request request = feign.Request.create(feign.Request.HttpMethod.GET,
                "http://course-service/internal?token=secret-token", java.util.Map.of(), null,
                java.nio.charset.StandardCharsets.UTF_8, null);
        throw new feign.FeignException.InternalServerError("secret-token failure", request, null, null);
    }

    @GetMapping("/denied")
    public String denied() {
        throw new AccessDeniedException("internal detail");
    }

    @PostMapping("/validate")
    public String validate(@Valid @RequestBody ProbeRequest request) {
        return request.name();
    }
}
