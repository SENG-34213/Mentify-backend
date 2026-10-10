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
class ExamResultSheetControllerTest {

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

    private ResultActions sheet(String bearer, UUID examId) throws Exception {
        var request = get("/api/v1/exams/" + examId + "/results");
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

    @Test
    void adminSuperAdminAndAssignedTeacherCanRetrieve() throws Exception {
        for (String t : List.of("a", "sa", "t")) {
            sheet(t, exam.getId()).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.examId").value(exam.getId().toString()))
                    .andExpect(jsonPath("$.data.courseId").value(COURSE_ID.toString()))
                    .andExpect(jsonPath("$.data.title").value("Midterm"))
                    .andExpect(jsonPath("$.data.status").value("MARKING"))
                    .andExpect(jsonPath("$.data.results.length()").value(3));
        }
    }

    @Test
    void unassignedTeacherStudentAndAnonymousRejected() throws Exception {
        sheet("t2", exam.getId()).andExpect(status().isForbidden());
        sheet("s", exam.getId()).andExpect(status().isForbidden());
        sheet(null, exam.getId()).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownExamReturns404() throws Exception {
        sheet("a", UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void courseServiceFailureDoesNotBypassTeacherAuthorization() throws Exception {
        when(courseServiceClient.getCourseById(eq(COURSE_ID), any())).thenThrow(new RuntimeException("boom"));
        var res = sheet("t", exam.getId());
        int code = res.andReturn().getResponse().getStatus();
        assertThat(code).isGreaterThanOrEqualTo(400);
        assertThat(res.andReturn().getResponse().getContentAsString()).doesNotContain("boom");
    }

    @Test
    void preservesStoredStateWithoutRecalculation() throws Exception {
        mark(studentA, AttendanceStatus.PRESENT, "80", ResultStatus.PASS);
        mark(studentB, AttendanceStatus.PRESENT, "0", ResultStatus.FAIL);
        ExamResult c = resultRepository.findByExamIdAndStudentId(exam.getId(), studentC).orElseThrow();
        c.setAttendanceStatus(AttendanceStatus.ABSENT);
        c.setGrade("B");
        c.setRemarks("sick");
        resultRepository.save(c);
        String json = sheet("a", exam.getId()).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (UUID s : List.of(studentA, studentB, studentC)) {
            assertThat(json).contains(s.toString());
        }
        sheet("a", exam.getId())
                .andExpect(jsonPath("$.data.results[?(@.studentId=='%s')].marksObtained".formatted(studentA)).value(80.0))
                .andExpect(jsonPath("$.data.results[?(@.studentId=='%s')].resultStatus".formatted(studentA)).value("PASS"))
                .andExpect(jsonPath("$.data.results[?(@.studentId=='%s')].marksObtained".formatted(studentB)).value(0.0))
                .andExpect(jsonPath("$.data.results[?(@.studentId=='%s')].resultStatus".formatted(studentB)).value("FAIL"))
                .andExpect(jsonPath("$.data.results[?(@.studentId=='%s')].attendanceStatus".formatted(studentC)).value("ABSENT"))
                .andExpect(jsonPath("$.data.results[?(@.studentId=='%s')].marksObtained".formatted(studentC)).value((Object) null))
                .andExpect(jsonPath("$.data.results[?(@.studentId=='%s')].resultStatus".formatted(studentC)).value("NOT_MARKED"))
                .andExpect(jsonPath("$.data.results[?(@.studentId=='%s')].grade".formatted(studentC)).value("B"))
                .andExpect(jsonPath("$.data.results[?(@.studentId=='%s')].remarks".formatted(studentC)).value("sick"));
    }

    @Test
    void retrievalIsReadOnlyAndWorksForCompletedExam() throws Exception {
        mark(studentA, AttendanceStatus.PRESENT, "80", ResultStatus.PASS);
        exam.setStatus(ExamStatus.COMPLETED);
        examRepository.save(exam);
        ExamResult before = resultRepository.findByExamIdAndStudentId(exam.getId(), studentA).orElseThrow();
        sheet("t", exam.getId()).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));
        ExamResult after = resultRepository.findByExamIdAndStudentId(exam.getId(), studentA).orElseThrow();
        assertThat(after.getMarksObtained()).isEqualByComparingTo(before.getMarksObtained());
        assertThat(after.getResultStatus()).isEqualTo(before.getResultStatus());
        assertThat(after.getUpdatedAt()).isEqualTo(before.getUpdatedAt());
        assertThat(examRepository.findById(exam.getId()).orElseThrow().getStatus()).isEqualTo(ExamStatus.COMPLETED);
        // unresolved records remain unresolved
        assertThat(stored(studentB).getResultStatus()).isEqualTo(ResultStatus.NOT_MARKED);
        assertThat(stored(studentB).getMarksObtained()).isNull();
    }

    @Test
    void onlyRequestedExamResultsAndEmptySheetHandled() throws Exception {
        Exam other = createExam(ExamStatus.MARKING);
        initResults(other, UUID.randomUUID());
        sheet("a", exam.getId()).andExpect(jsonPath("$.data.results.length()").value(3));
        Exam empty = createExam(ExamStatus.MARKING);
        sheet("a", empty.getId()).andExpect(status().isOk()).andExpect(jsonPath("$.data.results.length()").value(0));
    }

    private ExamResult stored(UUID student) {
        return resultRepository.findByExamIdAndStudentId(exam.getId(), student).orElseThrow();
    }
}