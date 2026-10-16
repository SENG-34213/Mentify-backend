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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ExamServiceApplication.class, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarksEntryControllerTest {

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
    void teacherMarksPassFailZeroAndAbsentInOneBatch() throws Exception {
        submit("t", exam.getId(), batch(
                entry(studentA, "PRESENT", "75"),
                entry(studentB, "PRESENT", "0"),
                entry(studentC, "ABSENT", "null")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(3));

        ExamResult a = stored(studentA);
        assertThat(a.getResultStatus()).isEqualTo(ResultStatus.PASS);
        assertThat(a.getMarksObtained()).isEqualByComparingTo("75");
        assertThat(a.getMarkedBy()).isEqualTo(TEACHER_ID);
        assertThat(a.getMarkedAt()).isNotNull();
        assertThat(a.getRemarks()).isEqualTo("r");

        ExamResult b = stored(studentB);
        assertThat(b.getAttendanceStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(b.getMarksObtained()).isEqualByComparingTo("0");
        assertThat(b.getResultStatus()).isEqualTo(ResultStatus.FAIL);

        ExamResult c = stored(studentC);
        assertThat(c.getAttendanceStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(c.getMarksObtained()).isNull();
    }

    @Test
    void exactPassMarkIsPassAndTotalMarksAccepted() throws Exception {
        submit("a", exam.getId(), batch(entry(studentA, "PRESENT", "50"), entry(studentB, "PRESENT", "100")))
                .andExpect(status().isOk());
        assertThat(stored(studentA).getResultStatus()).isEqualTo(ResultStatus.PASS);
        assertThat(stored(studentB).getResultStatus()).isEqualTo(ResultStatus.PASS);
    }

    @Test
    void belowPassMarkIsFailAndClientCannotSetResultStatus() throws Exception {
        String json = "{\"results\":[{\"studentId\":\"" + studentA + "\",\"attendanceStatus\":\"PRESENT\","
                + "\"marksObtained\":40,\"resultStatus\":\"PASS\",\"markedBy\":\"" + UUID.randomUUID() + "\"}]}";
        submit("sa", exam.getId(), json).andExpect(status().isOk());
        assertThat(stored(studentA).getResultStatus()).isEqualTo(ResultStatus.FAIL);
        assertThat(stored(studentA).getMarkedBy()).isNotEqualTo(TEACHER_ID);
    }

    @Test
    void invalidEntryRollsBackWholeBatch() throws Exception {
        submit("t", exam.getId(), batch(
                entry(studentA, "PRESENT", "75"),
                entry(studentB, "PRESENT", "60"),
                entry(studentC, "PRESENT", "150")))
                .andExpect(status().isBadRequest());
        for (UUID s : List.of(studentA, studentB, studentC)) {
            assertThat(stored(s).getResultStatus()).isEqualTo(ResultStatus.NOT_MARKED);
            assertThat(stored(s).getMarksObtained()).isNull();
            assertThat(stored(s).getMarkedAt()).isNull();
        }
    }

    @Test
    void rejectsInvalidMarkCombinations() throws Exception {
        String[] bad = {
                batch(entry(studentA, "PRESENT", "-1")),
                batch(entry(studentA, "PRESENT", "101")),
                batch(entry(studentA, "PRESENT", "null")),
                batch(entry(studentA, "ABSENT", "40")),
                batch(entry(studentA, "PRESENT", "60"), entry(studentA, "PRESENT", "70")),
                batch(entry(UUID.randomUUID(), "PRESENT", "60")),
                "{\"results\":[]}"};
        for (String json : bad) {
            submit("t", exam.getId(), json).andExpect(status().isBadRequest());
        }
        assertThat(resultRepository.findByExamId(exam.getId())).hasSize(3)
                .allMatch(r -> r.getResultStatus() == ResultStatus.NOT_MARKED);
    }

    @Test
    void examsOutsideMarkingStatusRejectMarks() throws Exception {
        for (ExamStatus s : List.of(ExamStatus.DRAFT, ExamStatus.SCHEDULED, ExamStatus.COMPLETED,
                ExamStatus.CANCELLED)) {
            Exam other = createExam(s);
            initResults(other, studentA);
            submit("t", other.getId(), batch(entry(studentA, "PRESENT", "60"))).andExpect(status().isConflict());
        }
    }

    @Test
    void authorizationRules() throws Exception {
        String json = batch(entry(studentA, "PRESENT", "60"));
        submit("s", exam.getId(), json).andExpect(status().isForbidden());
        submit("t2", exam.getId(), json).andExpect(status().isForbidden());
        submit(null, exam.getId(), json).andExpect(status().isUnauthorized());
        assertThat(stored(studentA).getResultStatus()).isEqualTo(ResultStatus.NOT_MARKED);
    }

    @Test
    void unknownExamReturnsNotFound() throws Exception {
        submit("a", UUID.randomUUID(), batch(entry(studentA, "PRESENT", "60"))).andExpect(status().isNotFound());
    }
}
