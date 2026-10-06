package com.mentify.exam.controller;

import com.mentify.ExamServiceApplication;
import com.mentify.exam.client.CourseServiceClient;
import com.mentify.exam.client.dto.CourseLookupResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamResult;
import com.mentify.exam.enums.AttendanceStatus;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.enums.ExamType;
import com.mentify.exam.enums.ResultStatus;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.repository.ExamResultRepository;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ExamServiceApplication.class, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExamCompletionControllerTest {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @Autowired ExamRepository examRepository;
    @Autowired ExamResultRepository resultRepository;
    @MockBean JwtDecoder jwtDecoder;
    @MockBean CourseServiceClient courseServiceClient;

    private final UUID studentA = UUID.randomUUID();
    private final UUID studentB = UUID.randomUUID();
    private final UUID studentC = UUID.randomUUID();
    private Exam exam;

    @BeforeEach
    void setUp() {
        resultRepository.deleteAll();
        examRepository.deleteAll();
        when(courseServiceClient.getCourseById(eq(COURSE_ID), any())).thenReturn(
                ApiResponse.success(200, "ok", new CourseLookupResponse(COURSE_ID, "Math", TEACHER_ID)));
        token("t", TEACHER_ID, "TEACHER");
        token("a", UUID.randomUUID(), "ADMIN");
        token("sa", UUID.randomUUID(), "SUPER_ADMIN");
        token("s", UUID.randomUUID(), "STUDENT");
        token("t2", UUID.randomUUID(), "TEACHER");
        exam = createExam(ExamStatus.MARKING);
        initResults(exam, studentA, studentB, studentC);
    }

    private void token(String name, UUID subject, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject.toString());
        claims.put("realm_access", Map.of("roles", List.of(role)));
        when(jwtDecoder.decode(name)).thenReturn(new Jwt(name, Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), claims));
    }

    private Exam createExam(ExamStatus status) {
        return examRepository.save(Exam.builder().courseId(COURSE_ID).title("Midterm")
                .examDate(LocalDate.of(2030, 1, 1)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(11, 0))
                .totalMarks(new BigDecimal("100")).passMarks(new BigDecimal("50")).type(ExamType.MIDTERM)
                .status(status).createdBy(TEACHER_ID).build());
    }

    private void initResults(Exam target, UUID... students) {
        for (UUID s : students) {
            resultRepository.save(ExamResult.builder().examId(target.getId()).studentId(s).build());
        }
    }

    private ResultActions submit(String bearer, UUID examId, String json) throws Exception {
        var request = put("/api/v1/exams/" + examId + "/results")
                .contentType(MediaType.APPLICATION_JSON).content(json);
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return mockMvc.perform(request);
    }

    private ResultActions complete(String bearer, UUID examId) throws Exception {
        var request = post("/api/v1/exams/" + examId + "/complete");
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return mockMvc.perform(request);
    }

    private void mark(UUID student, AttendanceStatus att, String marks, ResultStatus rs) {
        ExamResult r = resultRepository.findByExamIdAndStudentId(exam.getId(), student).orElseThrow();
        r.setAttendanceStatus(att);
        r.setMarksObtained(marks == null ? null : new BigDecimal(marks));
        r.setResultStatus(rs);
        resultRepository.save(r);
    }

    private void resolveAll() {
        mark(studentA, AttendanceStatus.PRESENT, "80", ResultStatus.PASS);
        mark(studentB, AttendanceStatus.PRESENT, "0", ResultStatus.FAIL);
        mark(studentC, AttendanceStatus.ABSENT, null, ResultStatus.NOT_MARKED);
    }

    private ExamStatus statusOf(Exam e) {
        return examRepository.findById(e.getId()).orElseThrow().getStatus();
    }

    private String entry(UUID student, String attendance, String marks) {
        return """
                {"studentId":"%s","attendanceStatus":"%s","marksObtained":%s,"remarks":"r"}"""
                .formatted(student, attendance, marks);
    }

    private String batch(String... entries) {
        return "{\"results\":[" + String.join(",", entries) + "]}";
    }

    private ExamResult stored(UUID student) {
        return resultRepository.findByExamIdAndStudentId(exam.getId(), student).orElseThrow();
    }

    @Test
    void adminSuperAdminAndAssignedTeacherCanCompleteResolvedExam() throws Exception {
        for (String who : List.of("a", "sa", "t")) {
            Exam e = createExam(ExamStatus.MARKING);
            initResults(e, studentA);
            ExamResult r = resultRepository.findByExamIdAndStudentId(e.getId(), studentA).orElseThrow();
            r.setAttendanceStatus(AttendanceStatus.PRESENT);
            r.setMarksObtained(new BigDecimal("30"));
            r.setResultStatus(ResultStatus.FAIL);
            resultRepository.save(r);
            complete(who, e.getId()).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
            assertThat(statusOf(e)).isEqualTo(ExamStatus.COMPLETED);
        }
    }

    @Test
    void mixedPresentZeroFailAndAbsentCompleteWithoutChangingResults() throws Exception {
        resolveAll();
        complete("t", exam.getId()).andExpect(status().isOk());
        assertThat(statusOf(exam)).isEqualTo(ExamStatus.COMPLETED);
        assertThat(stored(studentA).getMarksObtained()).isEqualByComparingTo("80");
        assertThat(stored(studentA).getResultStatus()).isEqualTo(ResultStatus.PASS);
        assertThat(stored(studentB).getMarksObtained()).isEqualByComparingTo("0");
        assertThat(stored(studentB).getResultStatus()).isEqualTo(ResultStatus.FAIL);
        assertThat(stored(studentC).getAttendanceStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(stored(studentC).getMarksObtained()).isNull();
    }

    @Test
    void unresolvedStudentBlocksCompletionAndIsNotAutoResolved() throws Exception {
        mark(studentA, AttendanceStatus.PRESENT, "80", ResultStatus.PASS);
        mark(studentB, AttendanceStatus.ABSENT, null, ResultStatus.NOT_MARKED);
        complete("a", exam.getId()).andExpect(status().isConflict());
        assertThat(statusOf(exam)).isEqualTo(ExamStatus.MARKING);
        assertThat(stored(studentC).getAttendanceStatus()).isNull();
        assertThat(stored(studentC).getMarksObtained()).isNull();
        assertThat(stored(studentC).getResultStatus()).isEqualTo(ResultStatus.NOT_MARKED);
    }

    @Test
    void examWithNoInitializedResultsCannotComplete() throws Exception {
        Exam e = createExam(ExamStatus.MARKING);
        complete("a", e.getId()).andExpect(status().isConflict());
        assertThat(statusOf(e)).isEqualTo(ExamStatus.MARKING);
    }

    @Test
    void onlyMarkingExamsCanBeCompleted() throws Exception {
        for (ExamStatus s : List.of(ExamStatus.DRAFT, ExamStatus.SCHEDULED, ExamStatus.CANCELLED,
                ExamStatus.COMPLETED)) {
            Exam e = createExam(s);
            complete("a", e.getId()).andExpect(status().isConflict());
            assertThat(statusOf(e)).isEqualTo(s);
        }
    }

    @Test
    void authorizationRules() throws Exception {
        resolveAll();
        complete("s", exam.getId()).andExpect(status().isForbidden());
        complete("t2", exam.getId()).andExpect(status().isForbidden());
        complete(null, exam.getId()).andExpect(status().isUnauthorized());
        assertThat(statusOf(exam)).isEqualTo(ExamStatus.MARKING);
    }

    @Test
    void unknownExamReturnsNotFound() throws Exception {
        complete("a", UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void completedExamAndResultsAreReadOnly() throws Exception {
        resolveAll();
        complete("a", exam.getId()).andExpect(status().isOk());
        submit("a", exam.getId(), batch(entry(studentA, "PRESENT", "10"))).andExpect(status().isConflict());
        assertThat(stored(studentA).getMarksObtained()).isEqualByComparingTo("80");
        String update = "{\"title\":\"X\",\"examDate\":\"2030-01-01\",\"startTime\":\"09:00\","
                + "\"endTime\":\"11:00\",\"totalMarks\":100,\"passMarks\":50,\"type\":\"MIDTERM\"}";
        mockMvc.perform(put("/api/v1/exams/" + exam.getId()).header("Authorization", "Bearer a")
                .contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isConflict());
    }
}