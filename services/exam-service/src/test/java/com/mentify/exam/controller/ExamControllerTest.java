package com.mentify.exam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.ExamServiceApplication;
import com.mentify.exam.client.CourseServiceClient;
import com.mentify.exam.client.dto.CourseLookupResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import feign.Request;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ExamServiceApplication.class, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExamControllerTest {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @Autowired ExamRepository examRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean JwtDecoder jwtDecoder;
    @MockBean CourseServiceClient courseServiceClient;

    @BeforeEach
    void setUp() {
        examRepository.deleteAll();
        stubCourse(TEACHER_ID);
    }

    private void stubCourse(UUID assignedTeacher) {
        when(courseServiceClient.getCourseById(eq(COURSE_ID), any())).thenReturn(
                ApiResponse.success(200, "ok", new CourseLookupResponse(COURSE_ID, "Math", assignedTeacher)));
    }

    private void token(String name, UUID subject, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject.toString());
        claims.put("realm_access", Map.of("roles", List.of(role)));
        when(jwtDecoder.decode(name)).thenReturn(new Jwt(name, Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), claims));
    }

    private String body(String title, String total, String pass, String start, String end, String type) {
        return """
                {"courseId":"%s","title":"%s","description":"d","examDate":"2030-01-01",
                 "startTime":"%s","endTime":"%s","totalMarks":%s,"passMarks":%s,"type":"%s",
                 "status":"COMPLETED","createdBy":"%s"}""".formatted(COURSE_ID, title, start, end, total, pass, type,
                UUID.randomUUID());
    }

    private String create(String bearer) throws Exception {
        String json = mockMvc.perform(post("/api/v1/exams").header("Authorization", "Bearer " + bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Midterm", "100", "40", "09:00:00", "11:00:00", "MIDTERM")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).at("/data/id").asText();
    }

    @Test
    void teacherCreatesDraftExamWithServerControlledFields() throws Exception {
        token("t", TEACHER_ID, "TEACHER");
        mockMvc.perform(post("/api/v1/exams").header("Authorization", "Bearer t")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Midterm", "100", "40", "09:00:00", "11:00:00", "MIDTERM")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.createdBy").value(TEACHER_ID.toString()));
    }

    @Test
    void studentIsForbidden() throws Exception {
        token("s", UUID.randomUUID(), "STUDENT");
        mockMvc.perform(post("/api/v1/exams").header("Authorization", "Bearer s")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Midterm", "100", "40", "09:00:00", "11:00:00", "MIDTERM")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/exams/" + UUID.randomUUID()).header("Authorization", "Bearer s"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unassignedTeacherIsForbidden() throws Exception {
        token("t2", UUID.randomUUID(), "TEACHER");
        mockMvc.perform(post("/api/v1/exams").header("Authorization", "Bearer t2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Midterm", "100", "40", "09:00:00", "11:00:00", "MIDTERM")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAnyCourse() throws Exception {
        token("a", UUID.randomUUID(), "ADMIN");
        String id = create("a");
        mockMvc.perform(get("/api/v1/exams/" + id).header("Authorization", "Bearer a"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/exams/courses/" + COURSE_ID).header("Authorization", "Bearer a"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void rejectsInvalidInput() throws Exception {
        token("t", TEACHER_ID, "TEACHER");
        String[][] bad = {
                {"X", "0", "0", "09:00:00", "11:00:00", "MIDTERM"},
                {"X", "100", "101", "09:00:00", "11:00:00", "MIDTERM"},
                {"X", "100", "-1", "09:00:00", "11:00:00", "MIDTERM"},
                {"X", "100", "40", "11:00:00", "09:00:00", "MIDTERM"},
                {" ", "100", "40", "09:00:00", "11:00:00", "MIDTERM"},
                {"X", "100", "40", "09:00:00", "11:00:00", "QUIZ"}};
        for (String[] b : bad) {
            mockMvc.perform(post("/api/v1/exams").header("Authorization", "Bearer t")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(b[0], b[1], b[2], b[3], b[4], b[5])))
                    .andExpect(status().isBadRequest());
        }
        assertThat(examRepository.count()).isZero();
    }

    @Test
    void unknownExamIs404() throws Exception {
        token("t", TEACHER_ID, "TEACHER");
        mockMvc.perform(get("/api/v1/exams/" + UUID.randomUUID()).header("Authorization", "Bearer t"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingCourseIs404AndCourseOutageFailsClosed() throws Exception {
        token("t", TEACHER_ID, "TEACHER");
        Request req = Request.create(Request.HttpMethod.GET, "/x", Map.of(), null, null, null);
        when(courseServiceClient.getCourseById(eq(COURSE_ID), any()))
                .thenThrow(new FeignException.NotFound("nf", req, null, null));
        String json = body("Midterm", "100", "40", "09:00:00", "11:00:00", "MIDTERM");
        mockMvc.perform(post("/api/v1/exams").header("Authorization", "Bearer t")
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isNotFound());

        when(courseServiceClient.getCourseById(eq(COURSE_ID), any()))
                .thenThrow(new FeignException.InternalServerError("boom", req, null, null));
        mockMvc.perform(post("/api/v1/exams").header("Authorization", "Bearer t")
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isServiceUnavailable());
    }

    @Test
    void updateAndCancelLifecycle() throws Exception {
        token("t", TEACHER_ID, "TEACHER");
        String id = create("t");
        String update = """
                {"title":"Updated","description":"d","examDate":"2030-02-01","startTime":"10:00:00",
                 "endTime":"12:00:00","totalMarks":80,"passMarks":30,"type":"FINAL"}""";
        mockMvc.perform(put("/api/v1/exams/" + id).header("Authorization", "Bearer t")
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("Updated"))
                .andExpect(jsonPath("$.data.type").value("FINAL"));
        mockMvc.perform(post("/api/v1/exams/" + id + "/cancel").header("Authorization", "Bearer t"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        mockMvc.perform(post("/api/v1/exams/" + id + "/cancel").header("Authorization", "Bearer t"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/exams/" + id).header("Authorization", "Bearer t")
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isConflict());
    }

    @Test
    void markingExamCannotBeUpdatedOrCancelled() throws Exception {
        token("t", TEACHER_ID, "TEACHER");
        UUID id = UUID.fromString(create("t"));
        Exam exam = examRepository.findById(id).orElseThrow();
        exam.setStatus(ExamStatus.MARKING);
        examRepository.save(exam);
        mockMvc.perform(post("/api/v1/exams/" + id + "/cancel").header("Authorization", "Bearer t"))
                .andExpect(status().isConflict());
    }
}
