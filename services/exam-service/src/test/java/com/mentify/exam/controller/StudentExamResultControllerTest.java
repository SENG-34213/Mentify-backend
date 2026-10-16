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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ExamServiceApplication.class, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentExamResultControllerTest {

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
    

    @BeforeEach
    void setUp() {
        resultRepository.deleteAll();
        examRepository.deleteAll();
        when(courseServiceClient.getCourseById(eq(COURSE_ID), any())).thenReturn(
                ApiResponse.success(200, "ok", new CourseLookupResponse(COURSE_ID, "Math", TEACHER_ID)));
        token("t", TEACHER_ID, "TEACHER");
        token("a", UUID.randomUUID(), "ADMIN");
        token("sa", UUID.randomUUID(), "SUPER_ADMIN");
        token("s", studentA, "STUDENT");token("sb", studentB, "STUDENT");
        token("t2", UUID.randomUUID(), "TEACHER");

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

    private ResultActions me(String bearer, String query) throws Exception {
        var request = get("/api/v1/exams/me" + query);
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return mockMvc.perform(request);
    }

    private void result(Exam e, UUID student, AttendanceStatus att, String marks, ResultStatus rs, String grade) {
        resultRepository.save(ExamResult.builder().examId(e.getId()).studentId(student).attendanceStatus(att)
                .marksObtained(marks == null ? null : new BigDecimal(marks)).resultStatus(rs).grade(grade)
                .remarks("rem").markedBy(TEACHER_ID).build());
    }

    @Test
    void returnsOnlyOwnCompletedResultsWithMarksSemantics() throws Exception {
        Exam pass = createExam(ExamStatus.COMPLETED);
        Exam zero = createExam(ExamStatus.COMPLETED);
        Exam absent = createExam(ExamStatus.COMPLETED);
        result(pass, studentA, AttendanceStatus.PRESENT, "80", ResultStatus.PASS, "A");
        result(zero, studentA, AttendanceStatus.PRESENT, "0", ResultStatus.FAIL, null);
        result(absent, studentA, AttendanceStatus.ABSENT, null, ResultStatus.NOT_MARKED, null);
        result(pass, studentB, AttendanceStatus.PRESENT, "95", ResultStatus.PASS, null);
        String json = me("s", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results.length()").value(3))
                .andExpect(jsonPath("$.data.results[?(@.examId=='%s')].marksObtained".formatted(pass.getId())).value(80.0))
                .andExpect(jsonPath("$.data.results[?(@.examId=='%s')].resultStatus".formatted(pass.getId())).value("PASS"))
                .andExpect(jsonPath("$.data.results[?(@.examId=='%s')].grade".formatted(pass.getId())).value("A"))
                .andExpect(jsonPath("$.data.results[?(@.examId=='%s')].marksObtained".formatted(zero.getId())).value(0.0))
                .andExpect(jsonPath("$.data.results[?(@.examId=='%s')].attendanceStatus".formatted(absent.getId())).value("ABSENT"))
                .andExpect(jsonPath("$.data.results[?(@.examId=='%s')].marksObtained".formatted(absent.getId())).value((Object) null))
                .andExpect(jsonPath("$.data.results[0].courseId").value(COURSE_ID.toString()))
                .andReturn().getResponse().getContentAsString();
        assertThat(json).doesNotContain("markedBy").doesNotContain("\"marksObtained\":95");
    }

    @Test
    void hidesNonCompletedExamsAndEmptyIsOk() throws Exception {
        for (ExamStatus st : List.of(ExamStatus.DRAFT, ExamStatus.SCHEDULED, ExamStatus.MARKING, ExamStatus.CANCELLED)) {
            result(createExam(st), studentA, AttendanceStatus.PRESENT, "70", ResultStatus.PASS, null);
        }
        me("s", "").andExpect(status().isOk()).andExpect(jsonPath("$.data.results.length()").value(0));
        result(createExam(ExamStatus.COMPLETED), studentA, AttendanceStatus.PRESENT, "70", ResultStatus.PASS, null);
        me("s", "").andExpect(jsonPath("$.data.results.length()").value(1));
    }

    @Test
    void studentIdParameterIsIgnoredAndIdentityComesFromJwt() throws Exception {
        Exam e = createExam(ExamStatus.COMPLETED);
        result(e, studentA, AttendanceStatus.PRESENT, "80", ResultStatus.PASS, null);
        result(e, studentB, AttendanceStatus.PRESENT, "95", ResultStatus.PASS, null);
        me("s", "?studentId=" + studentB).andExpect(jsonPath("$.data.results.length()").value(1))
                .andExpect(jsonPath("$.data.results[0].marksObtained").value(80.0));
        me("sb", "").andExpect(jsonPath("$.data.results[0].marksObtained").value(95.0));
    }

    @Test
    void retrievalDoesNotModifyData() throws Exception {
        Exam e = createExam(ExamStatus.COMPLETED);
        result(e, studentA, AttendanceStatus.PRESENT, "30", ResultStatus.PASS, null);
        me("s", "").andExpect(status().isOk());
        ExamResult r = resultRepository.findByExamIdAndStudentId(e.getId(), studentA).orElseThrow();
        assertThat(r.getResultStatus()).isEqualTo(ResultStatus.PASS);
        assertThat(r.getMarksObtained()).isEqualByComparingTo("30");
    }

    @Test
    void nonStudentsAndUnauthenticatedRejected() throws Exception {
        me("t", "").andExpect(status().isForbidden());
        me("a", "").andExpect(status().isForbidden());
        me("sa", "").andExpect(status().isForbidden());
        me(null, "").andExpect(status().isUnauthorized());
        when(jwtDecoder.decode("bad")).thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("bad"));
        me("bad", "").andExpect(status().isUnauthorized());
    }
}