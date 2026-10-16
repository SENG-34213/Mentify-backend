package com.mentify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.dto.GradeRequest;
import com.mentify.dto.GradeResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.GradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = GradeController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class GradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GradeService gradeService;

    @Test
    void createGrade_whenRequestIsValid_returnsCreatedGrade() throws Exception {
        UUID gradeId = UUID.randomUUID();
        when(gradeService.createGrade(any(GradeRequest.class))).thenReturn(
                ApiResponse.<GradeResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Grade created successfully")
                        .data(gradeResponse(gradeId, "Grade 3"))
                        .build()
        );

        mockMvc.perform(post("/api/v1/course/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Grade 3"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Grade created successfully"))
                .andExpect(jsonPath("$.data.id").value(gradeId.toString()))
                .andExpect(jsonPath("$.data.name").value("Grade 3"));

        verify(gradeService).createGrade(any(GradeRequest.class));
    }

    @Test
    void createGrade_whenNameIsBlank_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/course/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(" "))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(gradeService);
    }

    @Test
    void updateGrade_whenRequestIsValid_returnsUpdatedGrade() throws Exception {
        UUID gradeId = UUID.randomUUID();
        when(gradeService.updateGrade(eq(gradeId), any(GradeRequest.class))).thenReturn(
                ApiResponse.<GradeResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Grade updated successfully")
                        .data(gradeResponse(gradeId, "Grade 4"))
                        .build()
        );

        mockMvc.perform(put("/api/v1/course/grades/{gradeId}", gradeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Grade 4"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Grade updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Grade 4"));

        verify(gradeService).updateGrade(eq(gradeId), any(GradeRequest.class));
    }

    @Test
    void getGradeById_returnsGrade() throws Exception {
        UUID gradeId = UUID.randomUUID();
        when(gradeService.getGradeById(gradeId)).thenReturn(
                ApiResponse.<GradeResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Grade fetched successfully")
                        .data(gradeResponse(gradeId, "Grade 5"))
                        .build()
        );

        mockMvc.perform(get("/api/v1/course/grades/{gradeId}", gradeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Grade 5"));

        verify(gradeService).getGradeById(gradeId);
    }

    @Test
    void getAllGrades_returnsGrades() throws Exception {
        when(gradeService.getAllGrades()).thenReturn(
                ApiResponse.<List<GradeResponse>>builder()
                        .status(HttpStatus.OK)
                        .message("Grades fetched successfully")
                        .data(List.of(gradeResponse(UUID.randomUUID(), "Grade 3")))
                        .build()
        );

        mockMvc.perform(get("/api/v1/course/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(gradeService).getAllGrades();
    }

    @Test
    void deleteGrade_returnsOk() throws Exception {
        UUID gradeId = UUID.randomUUID();
        when(gradeService.deleteGrade(gradeId)).thenReturn(
                ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message("Grade deleted successfully")
                        .build()
        );

        mockMvc.perform(delete("/api/v1/course/grades/{gradeId}", gradeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Grade deleted successfully"));

        verify(gradeService).deleteGrade(gradeId);
    }

    @Test
    void mutatingEndpointsHaveAdminAndSuperAdminPreAuthorize() throws NoSuchMethodException {
        Method create = GradeController.class.getDeclaredMethod("createGrade", GradeRequest.class);
        Method update = GradeController.class.getDeclaredMethod("updateGrade", UUID.class, GradeRequest.class);
        Method delete = GradeController.class.getDeclaredMethod("deleteGrade", UUID.class);

        assertThat(create.getAnnotation(PreAuthorize.class).value()).contains("ADMIN", "SUPER_ADMIN");
        assertThat(update.getAnnotation(PreAuthorize.class).value()).contains("ADMIN", "SUPER_ADMIN");
        assertThat(delete.getAnnotation(PreAuthorize.class).value()).contains("ADMIN", "SUPER_ADMIN");
    }

    private GradeRequest validRequest(String name) {
        return GradeRequest.builder()
                .name(name)
                .description("Primary grade")
                .build();
    }

    private GradeResponse gradeResponse(UUID gradeId, String name) {
        return GradeResponse.builder()
                .id(gradeId)
                .name(name)
                .description("Primary grade")
                .active(true)
                .build();
    }
}
