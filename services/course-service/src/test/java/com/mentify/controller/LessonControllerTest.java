package com.mentify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.dto.LessonRequest;
import com.mentify.dto.LessonResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.LessonService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LessonController.class, properties = "spring.cloud.config.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
class LessonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LessonService lessonService;

    @Test
    void createLesson_whenValidRequest_returnsCreated() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        LessonResponse responseData = LessonResponse.builder().id(UUID.randomUUID()).title("Lesson 1").build();

        when(lessonService.createLesson(eq(courseId), eq(moduleId), any(LessonRequest.class))).thenReturn(
                ApiResponse.<LessonResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Lesson created successfully")
                        .data(responseData)
                        .build()
        );

        LessonRequest request = LessonRequest.builder().title("Lesson 1").build();

        mockMvc.perform(post("/api/v1/course/{courseId}/modules/{moduleId}/lessons", courseId, moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Lesson created successfully"));
    }

    @Test
    void updateLesson_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        LessonResponse responseData = LessonResponse.builder().id(lessonId).title("Lesson 2").build();

        when(lessonService.updateLesson(eq(courseId), eq(moduleId), eq(lessonId), any(LessonRequest.class))).thenReturn(
                ApiResponse.<LessonResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Lesson updated successfully")
                        .data(responseData)
                        .build()
        );

        LessonRequest request = LessonRequest.builder().title("Lesson 2").build();

        mockMvc.perform(put("/api/v1/course/{courseId}/modules/{moduleId}/lessons/{lessonId}", courseId, moduleId, lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lesson updated successfully"));
    }

    @Test
    void deleteLesson_whenExists_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        when(lessonService.deleteLesson(courseId, moduleId, lessonId)).thenReturn(
                ApiResponse.builder().status(HttpStatus.OK).message("Lesson deleted successfully").build()
        );

        mockMvc.perform(delete("/api/v1/course/{courseId}/modules/{moduleId}/lessons/{lessonId}", courseId, moduleId, lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lesson deleted successfully"));

        verify(lessonService).deleteLesson(courseId, moduleId, lessonId);
    }

    @Test
    void createLesson_whenTitleBlank_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        LessonRequest request = LessonRequest.builder().title(" ").build();

        mockMvc.perform(post("/api/v1/course/{courseId}/modules/{moduleId}/lessons", courseId, moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lessonService);
    }

    @Test
    void endpoints_haveTeacherPreAuthorize() throws NoSuchMethodException {
        Method createMethod = LessonController.class.getDeclaredMethod("createLesson", UUID.class, UUID.class, LessonRequest.class);
        Method updateMethod = LessonController.class.getDeclaredMethod("updateLesson", UUID.class, UUID.class, UUID.class, LessonRequest.class);
        Method deleteMethod = LessonController.class.getDeclaredMethod("deleteLesson", UUID.class, UUID.class, UUID.class);

        assertThat(createMethod.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('TEACHER')");
        assertThat(updateMethod.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('TEACHER')");
        assertThat(deleteMethod.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('TEACHER')");
    }
}
