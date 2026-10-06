package com.mentify.exam.controller;

import com.mentify.ExamServiceApplication;
import com.mentify.exam.client.CourseServiceClient;
import com.mentify.exam.client.EnrollmentServiceClient;
import com.mentify.exam.client.dto.CourseLookupResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.enums.ExamType;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ExamServiceApplication.class, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExamLifecycleControllerTest {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @Autowired ExamRepository examRepository;
    @Autowired ExamResultRepository resultRepository;
    @MockBean JwtDecoder jwtDecoder;
    @MockBean CourseServiceClient courseServiceClient;
    @MockBean EnrollmentServiceClient enrollmentServiceClient;

    private final UUID studentA = UUID.randomUUID();
    private final UUID studentB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        resultRepository.deleteAll();
        examRepository.deleteAll();
        when(courseServiceClient.getCourseById(eq(COURSE_ID), any())).thenReturn(
                ApiResponse.success(200, "ok", new CourseLookupResponse(COURSE_ID, "Math", TEACHER_ID)));
        when(enrollmentServiceClient.getEnrolledStudentIdsByCourse(eq(COURSE_ID), any()))
                .thenReturn(ApiResponse.success(200, "ok", List.of(studentA, studentB)));
        token("t", TEACHER_ID, "TEACHER");
        token("other", UUID.randomUUID(), "TEACHER");
        token("s", UUID.randomUUID(), "STUDENT");
    }

    private void token(String name, UUID subject, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject.toString());
        claims.put("realm_access", Map.of("roles", List.of(role)));
        when(jwtDecoder.decode(name)).thenReturn(new Jwt(name, Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), claims));
    }

    private Exam exam(ExamStatus status) {
        return examRepository.save(Exam.builder().courseId(COURSE_ID).title("Midterm")
                .examDate(LocalDate.of(2030, 1, 1)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(11, 0))
                .totalMarks(new BigDecimal("100")).passMarks(new BigDecimal("50")).type(ExamType.MIDTERM)
                .status(status).createdBy(TEACHER_ID).build());
    }

    private ResultActions call(String action, String bearer, UUID examId) throws Exception {
        var request = post("/api/v1/exams/" + examId + "/" + action);
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return mockMvc.perform(request);
    }

    private ExamStatus statusOf(Exam e) {
        return examRepository.findById(e.getId()).orElseThrow().getStatus();
    }

    @Test
    void scheduleMovesDraftToScheduledAndInitializesResults() throws Exception {
        Exam e = exam(ExamStatus.DRAFT);
        call("schedule", "t", e.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
        assertThat(resultRepository.findByExamId(e.getId())).hasSize(2);
    }

    @Test
    void startMarkingMovesScheduledToMarkingAndPicksUpLateEnrollments() throws Exception {
        Exam e = exam(ExamStatus.DRAFT);
        call("schedule", "t", e.getId()).andExpect(status().isOk());
        UUID late = UUID.randomUUID();
        when(enrollmentServiceClient.getEnrolledStudentIdsByCourse(eq(COURSE_ID), any()))
                .thenReturn(ApiResponse.success(200, "ok", List.of(studentA, studentB, late)));
        call("start-marking", "t", e.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MARKING"));
        assertThat(resultRepository.findByExamId(e.getId())).hasSize(3);
    }

    @Test
    void invalidTransitionsReturn409() throws Exception {
        Exam draft = exam(ExamStatus.DRAFT);
        call("start-marking", "t", draft.getId()).andExpect(status().isConflict());
        for (ExamStatus s : List.of(ExamStatus.SCHEDULED, ExamStatus.MARKING,
                ExamStatus.COMPLETED, ExamStatus.CANCELLED)) {
            call("schedule", "t", exam(s).getId()).andExpect(status().isConflict());
        }
        for (ExamStatus s : List.of(ExamStatus.MARKING, ExamStatus.COMPLETED, ExamStatus.CANCELLED)) {
            call("start-marking", "t", exam(s).getId()).andExpect(status().isConflict());
        }
        assertThat(statusOf(draft)).isEqualTo(ExamStatus.DRAFT);
    }

    @Test
    void enrollmentFailureReturns503AndLeavesExamUnchanged() throws Exception {
        Exam e = exam(ExamStatus.DRAFT);
        when(enrollmentServiceClient.getEnrolledStudentIdsByCourse(eq(COURSE_ID), any()))
                .thenThrow(FeignException.errorStatus("m", feign.Response.builder().status(500)
                        .request(feign.Request.create(feign.Request.HttpMethod.GET, "http://x",
                                Map.of(), null, null, null)).headers(Map.of()).build()));
        call("schedule", "t", e.getId()).andExpect(status().isServiceUnavailable());
        assertThat(statusOf(e)).isEqualTo(ExamStatus.DRAFT);
        assertThat(resultRepository.findByExamId(e.getId())).isEmpty();
    }

    @Test
    void securityAndNotFound() throws Exception {
        Exam e = exam(ExamStatus.DRAFT);
        call("schedule", null, e.getId()).andExpect(status().isUnauthorized());
        call("schedule", "s", e.getId()).andExpect(status().isForbidden());
        call("schedule", "other", e.getId()).andExpect(status().isForbidden());
        call("start-marking", "t", UUID.randomUUID()).andExpect(status().isNotFound());
        assertThat(statusOf(e)).isEqualTo(ExamStatus.DRAFT);
    }
}