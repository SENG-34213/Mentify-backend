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
class ExamSummaryControllerTest {

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

    private void mark(UUID student, AttendanceStatus att, String marks, ResultStatus rs) {
        ExamResult r = resultRepository.findByExamIdAndStudentId(exam.getId(), student).orElseThrow();
        r.setAttendanceStatus(att);
        r.setMarksObtained(marks == null ? null : new BigDecimal(marks));
        r.setResultStatus(rs);
        resultRepository.save(r);
    }

    private ResultActions summary(String bearer, UUID examId) throws Exception {
        var request = get("/api/v1/exams/" + examId + "/summary");
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return mockMvc.perform(request);
    }

    private void setStatus(ExamStatus s) {
        Exam e = examRepository.findById(exam.getId()).orElseThrow();
        e.setStatus(s);
        examRepository.save(e);
    }

    @Test
    void computesCountsAndStatsExcludingAbsentAndIncludingZero() throws Exception {
        UUID d = UUID.randomUUID();
        initResults(exam, d);
        mark(studentA, AttendanceStatus.PRESENT, "80", ResultStatus.PASS);
        mark(studentB, AttendanceStatus.PRESENT, "50", ResultStatus.PASS);
        mark(studentC, AttendanceStatus.PRESENT, "0", ResultStatus.FAIL);
        mark(d, AttendanceStatus.ABSENT, null, ResultStatus.NOT_MARKED);
        summary("a", exam.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examId").value(exam.getId().toString()))
                .andExpect(jsonPath("$.data.totalStudents").value(4))
                .andExpect(jsonPath("$.data.presentStudents").value(3))
                .andExpect(jsonPath("$.data.absentStudents").value(1))
                .andExpect(jsonPath("$.data.passedStudents").value(2))
                .andExpect(jsonPath("$.data.failedStudents").value(1))
                .andExpect(jsonPath("$.data.averageMarks").value(43.33))
                .andExpect(jsonPath("$.data.highestMarks").value(80.0))
                .andExpect(jsonPath("$.data.lowestMarks").value(0.0));
    }

    @Test
    void allStaffRolesAllowedOthersRejected() throws Exception {
        for (String t : List.of("a", "sa", "t")) {
            summary(t, exam.getId()).andExpect(status().isOk());
        }
        summary("t2", exam.getId()).andExpect(status().isForbidden());
        summary("s", exam.getId()).andExpect(status().isForbidden());
        summary(null, exam.getId()).andExpect(status().isUnauthorized());
        summary("a", UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void courseServiceFailureDoesNotBypassTeacherAuthorization() throws Exception {
        when(courseServiceClient.getCourseById(eq(COURSE_ID), any())).thenThrow(new RuntimeException("down"));
        summary("t", exam.getId()).andExpect(result ->
                assertThat(result.getResponse().getStatus()).isGreaterThanOrEqualTo(400));
    }

    @Test
    void noNumericMarksYieldsNullStatsWithoutError() throws Exception {
        mark(studentA, AttendanceStatus.ABSENT, null, ResultStatus.NOT_MARKED);
        summary("a", exam.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents").value(3))
                .andExpect(jsonPath("$.data.absentStudents").value(1))
                .andExpect(jsonPath("$.data.failedStudents").value(0))
                .andExpect(jsonPath("$.data.averageMarks").value((Object) null))
                .andExpect(jsonPath("$.data.highestMarks").value((Object) null))
                .andExpect(jsonPath("$.data.lowestMarks").value((Object) null));
    }

    @Test
    void examWithNoResultsReturnsEmptySummary() throws Exception {
        resultRepository.deleteAll();
        summary("a", exam.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents").value(0))
                .andExpect(jsonPath("$.data.averageMarks").value((Object) null));
    }

    @Test
    void completedAllowedCancelledRejected() throws Exception {
        mark(studentA, AttendanceStatus.PRESENT, "60", ResultStatus.PASS);
        setStatus(ExamStatus.COMPLETED);
        summary("a", exam.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.averageMarks").value(60.0));
        setStatus(ExamStatus.CANCELLED);
        summary("a", exam.getId()).andExpect(status().isConflict());
    }

    @Test
    void retrievalIsReadOnly() throws Exception {
        mark(studentA, AttendanceStatus.PRESENT, "30", ResultStatus.PASS);
        summary("a", exam.getId()).andExpect(status().isOk());
        ExamResult r = resultRepository.findByExamIdAndStudentId(exam.getId(), studentA).orElseThrow();
        assertThat(r.getResultStatus()).isEqualTo(ResultStatus.PASS);
        assertThat(r.getMarksObtained()).isEqualByComparingTo("30");
        assertThat(examRepository.findById(exam.getId()).orElseThrow().getStatus()).isEqualTo(ExamStatus.MARKING);
    }
}