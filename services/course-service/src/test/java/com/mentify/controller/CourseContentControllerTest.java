package com.mentify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.dto.LearningMaterialCreateRequest;
import com.mentify.dto.LearningMaterialResponse;
import com.mentify.dto.LessonCreateRequest;
import com.mentify.dto.LessonResponse;
import com.mentify.dto.ModuleCreateRequest;
import com.mentify.dto.ModuleResponse;
import com.mentify.enums.MaterialType;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.LearningMaterialService;
import com.mentify.service.LessonService;
import com.mentify.service.ModuleService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CourseContentController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class CourseContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
        private ModuleService moduleService;

        @MockBean
        private LessonService lessonService;

        @MockBean
        private LearningMaterialService learningMaterialService;

    @Test
    void createModule_whenValidRequest_returnsCreated() throws Exception {
        UUID courseId = UUID.randomUUID();
        ModuleResponse moduleResponse = ModuleResponse.builder()
                .id(UUID.randomUUID())
                .title("Algebra")
                .courseId(courseId)
                .sequenceOrder(1)
                .isVisible(true)
                .build();

        when(moduleService.createModule(eq(courseId), any(ModuleCreateRequest.class))).thenReturn(
                ApiResponse.<ModuleResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Module created successfully")
                        .data(moduleResponse)
                        .build()
        );

        ModuleCreateRequest request = ModuleCreateRequest.builder()
                .title("Algebra")
                .sequenceOrder(1)
                .build();

        mockMvc.perform(post("/api/v1/course/{courseId}/modules", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Module created successfully"))
                .andExpect(jsonPath("$.data.title").value("Algebra"));

        verify(moduleService).createModule(eq(courseId), any(ModuleCreateRequest.class));
    }

    @Test
    void createLesson_whenValidRequest_returnsCreated() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        LessonResponse lessonResponse = LessonResponse.builder()
                .id(UUID.randomUUID())
                .title("Linear Equations")
                .moduleId(moduleId)
                .isVisible(true)
                .build();

        when(lessonService.createLesson(eq(courseId), eq(moduleId), any(LessonCreateRequest.class))).thenReturn(
                ApiResponse.<LessonResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Lesson created successfully")
                        .data(lessonResponse)
                        .build()
        );

        LessonCreateRequest request = LessonCreateRequest.builder()
                .title("Linear Equations")
                .build();

        mockMvc.perform(post("/api/v1/course/{courseId}/modules/{moduleId}/lessons", courseId, moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Lesson created successfully"))
                .andExpect(jsonPath("$.data.title").value("Linear Equations"));

        verify(lessonService).createLesson(eq(courseId), eq(moduleId), any(LessonCreateRequest.class));
    }

    @Test
    void createLearningMaterial_whenValidRequest_returnsCreated() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        LearningMaterialResponse materialResponse = LearningMaterialResponse.builder()
                .id(UUID.randomUUID())
                .title("Intro Video")
                .type(MaterialType.VIDEO)
                .fileUrl("https://cdn.example.com/algebra.mp4")
                .moduleId(moduleId)
                .build();

        when(learningMaterialService.createLearningMaterial(eq(courseId), eq(moduleId), any(LearningMaterialCreateRequest.class)))
                .thenReturn(ApiResponse.<LearningMaterialResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Learning material created successfully")
                        .data(materialResponse)
                        .build());

        LearningMaterialCreateRequest request = LearningMaterialCreateRequest.builder()
                .title("Intro Video")
                .type(MaterialType.VIDEO)
                .fileUrl("https://cdn.example.com/algebra.mp4")
                .build();

        mockMvc.perform(post("/api/v1/course/{courseId}/modules/{moduleId}/learning-materials", courseId, moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Learning material created successfully"))
                .andExpect(jsonPath("$.data.title").value("Intro Video"));

        verify(learningMaterialService).createLearningMaterial(eq(courseId), eq(moduleId), any(LearningMaterialCreateRequest.class));
    }

    @Test
    void createModule_whenTitleIsBlank_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        ModuleCreateRequest request = ModuleCreateRequest.builder()
                .title(" ")
                .sequenceOrder(1)
                .build();

        mockMvc.perform(post("/api/v1/course/{courseId}/modules", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(moduleService, lessonService, learningMaterialService);
    }

    @Test
    void createModule_hasTeacherPreAuthorize() throws NoSuchMethodException {
        Method method = CourseContentController.class.getDeclaredMethod(
                "createModule",
                UUID.class,
                ModuleCreateRequest.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('TEACHER')");
    }

    @Test
    void createLesson_hasTeacherPreAuthorize() throws NoSuchMethodException {
        Method method = CourseContentController.class.getDeclaredMethod(
                "createLesson",
                UUID.class,
                UUID.class,
                LessonCreateRequest.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('TEACHER')");
    }

    @Test
    void createLearningMaterial_hasTeacherPreAuthorize() throws NoSuchMethodException {
        Method method = CourseContentController.class.getDeclaredMethod(
                "createLearningMaterial",
                UUID.class,
                UUID.class,
                LearningMaterialCreateRequest.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('TEACHER')");
    }
}
