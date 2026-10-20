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
import static org.mockito.Mockito.doThrow;
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
        controllers = CourseContentController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class CourseContentControllerTest {

    private static final String COURSE_CONTENT_ROLES = "hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')";
    private static final String COURSE_CONTENT_READ_ROLES = "hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')";
    private static final String AUTH_HEADER = "Bearer token";

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
                .durationMinutes(45)
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
                .durationMinutes(45)
                .build();

        mockMvc.perform(post("/api/v1/course/{courseId}/modules/{moduleId}/lessons", courseId, moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Lesson created successfully"))
                .andExpect(jsonPath("$.data.title").value("Linear Equations"))
                .andExpect(jsonPath("$.data.durationMinutes").value(45));

        verify(lessonService).createLesson(eq(courseId), eq(moduleId), any(LessonCreateRequest.class));
    }

    @Test
    void updateModule_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        ModuleResponse moduleResponse = ModuleResponse.builder()
                .id(moduleId)
                .title("Updated Algebra")
                .courseId(courseId)
                .sequenceOrder(2)
                .isVisible(true)
                .build();

        when(moduleService.updateModule(eq(courseId), eq(moduleId), any(ModuleCreateRequest.class))).thenReturn(
                ApiResponse.<ModuleResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Module updated successfully")
                        .data(moduleResponse)
                        .build()
        );

        ModuleCreateRequest request = ModuleCreateRequest.builder()
                .title("Updated Algebra")
                .sequenceOrder(2)
                .build();

        mockMvc.perform(put("/api/v1/course/{courseId}/modules/{moduleId}", courseId, moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Module updated successfully"))
                .andExpect(jsonPath("$.data.title").value("Updated Algebra"));

        verify(moduleService).updateModule(eq(courseId), eq(moduleId), any(ModuleCreateRequest.class));
    }

    @Test
    void updateModule_whenTitleIsBlank_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        ModuleCreateRequest request = ModuleCreateRequest.builder()
                .title(" ")
                .sequenceOrder(1)
                .build();

        mockMvc.perform(put("/api/v1/course/{courseId}/modules/{moduleId}", courseId, moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Module title is required"));

        verifyNoInteractions(moduleService, lessonService, learningMaterialService);
    }

    @Test
    void updateModule_whenModuleIdIsInvalidUuid_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        ModuleCreateRequest request = ModuleCreateRequest.builder()
                .title("Updated Algebra")
                .sequenceOrder(2)
                .build();

        mockMvc.perform(put("/api/v1/course/{courseId}/modules/{moduleId}", courseId, "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid UUID format for parameter 'moduleId'"));

        verifyNoInteractions(moduleService, lessonService, learningMaterialService);
    }

    @Test
    void updateLesson_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        LessonResponse lessonResponse = LessonResponse.builder()
                .id(lessonId)
                .title("Updated Linear Equations")
                .durationMinutes(60)
                .moduleId(moduleId)
                .isVisible(true)
                .build();

        when(lessonService.updateLesson(eq(courseId), eq(moduleId), eq(lessonId), any(LessonCreateRequest.class)))
                .thenReturn(ApiResponse.<LessonResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Lesson updated successfully")
                        .data(lessonResponse)
                        .build());

        LessonCreateRequest request = LessonCreateRequest.builder()
                .title("Updated Linear Equations")
                .durationMinutes(60)
                .build();

        mockMvc.perform(put("/api/v1/course/{courseId}/modules/{moduleId}/lessons/{lessonId}", courseId, moduleId, lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lesson updated successfully"))
                .andExpect(jsonPath("$.data.title").value("Updated Linear Equations"))
                .andExpect(jsonPath("$.data.durationMinutes").value(60));

        verify(lessonService).updateLesson(eq(courseId), eq(moduleId), eq(lessonId), any(LessonCreateRequest.class));
    }

    @Test
    void updateLesson_whenTitleIsBlank_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        LessonCreateRequest request = LessonCreateRequest.builder()
                .title(" ")
                .durationMinutes(45)
                .build();

        mockMvc.perform(put("/api/v1/course/{courseId}/modules/{moduleId}/lessons/{lessonId}", courseId, moduleId, lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lesson title is required"));

        verifyNoInteractions(moduleService, lessonService, learningMaterialService);
    }

    @Test
    void createLesson_whenDurationIsMissing_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        LessonCreateRequest request = LessonCreateRequest.builder()
                .title("Lesson")
                .build();

        mockMvc.perform(post("/api/v1/course/{courseId}/modules/{moduleId}/lessons", courseId, moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lesson duration is required"));

        verifyNoInteractions(moduleService, lessonService, learningMaterialService);
    }

    @Test
    void updateLesson_whenDurationIsZero_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        LessonCreateRequest request = LessonCreateRequest.builder()
                .title("Lesson")
                .durationMinutes(0)
                .build();

        mockMvc.perform(put("/api/v1/course/{courseId}/modules/{moduleId}/lessons/{lessonId}", courseId, moduleId, lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lesson duration must be at least 1 minute"));

        verifyNoInteractions(moduleService, lessonService, learningMaterialService);
    }

    @Test
    void updateLesson_whenLessonIdIsInvalidUuid_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        LessonCreateRequest request = LessonCreateRequest.builder()
                .title("Updated Linear Equations")
                .durationMinutes(45)
                .build();

        mockMvc.perform(put("/api/v1/course/{courseId}/modules/{moduleId}/lessons/{lessonId}", courseId, moduleId, "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid UUID format for parameter 'lessonId'"));

        verifyNoInteractions(moduleService, lessonService, learningMaterialService);
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

        when(learningMaterialService.createLearningMaterial(
                eq(courseId),
                eq(moduleId),
                any(LearningMaterialCreateRequest.class),
                eq(AUTH_HEADER)
        ))
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
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Learning material created successfully"))
                .andExpect(jsonPath("$.data.title").value("Intro Video"));

        verify(learningMaterialService).createLearningMaterial(
                eq(courseId),
                eq(moduleId),
                any(LearningMaterialCreateRequest.class),
                eq(AUTH_HEADER)
        );
    }

    @Test
    void updateLearningMaterial_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();

        LearningMaterialResponse materialResponse = LearningMaterialResponse.builder()
                .id(materialId)
                .title("Updated Intro Video")
                .type(MaterialType.VIDEO)
                .fileUrl("https://cdn.example.com/algebra-v2.mp4")
                .moduleId(moduleId)
                .build();

        when(learningMaterialService.updateLearningMaterial(
                eq(courseId),
                eq(moduleId),
                eq(materialId),
                any(LearningMaterialCreateRequest.class),
                eq(AUTH_HEADER)
        )).thenReturn(ApiResponse.<LearningMaterialResponse>builder()
                .status(HttpStatus.OK)
                .message("Learning material updated successfully")
                .data(materialResponse)
                .build());

        LearningMaterialCreateRequest request = LearningMaterialCreateRequest.builder()
                .title("Updated Intro Video")
                .type(MaterialType.VIDEO)
                .fileUrl("https://cdn.example.com/algebra-v2.mp4")
                .build();

        mockMvc.perform(put("/api/v1/course/{courseId}/modules/{moduleId}/learning-materials/{materialId}", courseId, moduleId, materialId)
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Learning material updated successfully"))
                .andExpect(jsonPath("$.data.title").value("Updated Intro Video"));

        verify(learningMaterialService).updateLearningMaterial(
                eq(courseId),
                eq(moduleId),
                eq(materialId),
                any(LearningMaterialCreateRequest.class),
                eq(AUTH_HEADER)
        );
    }

    @Test
    void updateLearningMaterial_whenTypeIsMissing_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        LearningMaterialCreateRequest request = LearningMaterialCreateRequest.builder()
                .title("Updated Intro Video")
                .fileUrl("https://cdn.example.com/algebra-v2.mp4")
                .build();

        doThrow(new IllegalArgumentException("Learning material type is required for legacy fileUrl materials"))
                .when(learningMaterialService)
                .updateLearningMaterial(
                        eq(courseId),
                        eq(moduleId),
                        eq(materialId),
                        any(LearningMaterialCreateRequest.class),
                        eq(AUTH_HEADER)
                );

        mockMvc.perform(put(
                        "/api/v1/course/{courseId}/modules/{moduleId}/learning-materials/{materialId}",
                        courseId,
                        moduleId,
                        materialId
                )
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Learning material type is required for legacy fileUrl materials"));

        verify(learningMaterialService).updateLearningMaterial(
                eq(courseId),
                eq(moduleId),
                eq(materialId),
                any(LearningMaterialCreateRequest.class),
                eq(AUTH_HEADER)
        );
    }

    @Test
    void updateLearningMaterial_whenMaterialIdIsInvalidUuid_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        LearningMaterialCreateRequest request = LearningMaterialCreateRequest.builder()
                .title("Updated Intro Video")
                .type(MaterialType.VIDEO)
                .fileUrl("https://cdn.example.com/algebra-v2.mp4")
                .build();

        mockMvc.perform(put(
                        "/api/v1/course/{courseId}/modules/{moduleId}/learning-materials/{materialId}",
                        courseId,
                        moduleId,
                        "not-a-uuid"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid UUID format for parameter 'materialId'"));

        verifyNoInteractions(moduleService, lessonService, learningMaterialService);
    }

    @Test
    void getModules_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        ModuleResponse moduleResponse = ModuleResponse.builder()
                .id(UUID.randomUUID())
                .title("Algebra")
                .courseId(courseId)
                .build();

        when(moduleService.getModules(courseId)).thenReturn(ApiResponse.<java.util.List<ModuleResponse>>builder()
                .status(HttpStatus.OK)
                .message("Modules fetched successfully")
                .data(java.util.List.of(moduleResponse))
                .build());

        mockMvc.perform(get("/api/v1/course/{courseId}/modules", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Modules fetched successfully"))
                .andExpect(jsonPath("$.data[0].title").value("Algebra"));

        verify(moduleService).getModules(courseId);
    }

    @Test
    void getModule_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        when(moduleService.getModule(courseId, moduleId)).thenReturn(ApiResponse.<ModuleResponse>builder()
                .status(HttpStatus.OK)
                .message("Module fetched successfully")
                .data(ModuleResponse.builder().id(moduleId).title("Algebra").courseId(courseId).build())
                .build());

        mockMvc.perform(get("/api/v1/course/{courseId}/modules/{moduleId}", courseId, moduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Module fetched successfully"));

        verify(moduleService).getModule(courseId, moduleId);
    }

    @Test
    void getLessons_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        when(lessonService.getLessons(courseId, moduleId)).thenReturn(ApiResponse.<java.util.List<LessonResponse>>builder()
                .status(HttpStatus.OK)
                .message("Lessons fetched successfully")
                .data(java.util.List.of(LessonResponse.builder().id(UUID.randomUUID()).title("L1").moduleId(moduleId).build()))
                .build());

        mockMvc.perform(get("/api/v1/course/{courseId}/modules/{moduleId}/lessons", courseId, moduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lessons fetched successfully"));

        verify(lessonService).getLessons(courseId, moduleId);
    }

    @Test
    void getLesson_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        when(lessonService.getLesson(courseId, moduleId, lessonId)).thenReturn(ApiResponse.<LessonResponse>builder()
                .status(HttpStatus.OK)
                .message("Lesson fetched successfully")
                .data(LessonResponse.builder().id(lessonId).title("L1").moduleId(moduleId).build())
                .build());

        mockMvc.perform(get("/api/v1/course/{courseId}/modules/{moduleId}/lessons/{lessonId}", courseId, moduleId, lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lesson fetched successfully"));

        verify(lessonService).getLesson(courseId, moduleId, lessonId);
    }

    @Test
    void getLearningMaterials_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        when(learningMaterialService.getLearningMaterials(courseId, moduleId, AUTH_HEADER))
                .thenReturn(ApiResponse.<java.util.List<LearningMaterialResponse>>builder()
                        .status(HttpStatus.OK)
                        .message("Learning materials fetched successfully")
                        .data(java.util.List.of(LearningMaterialResponse.builder()
                                .id(UUID.randomUUID())
                                .title("Doc")
                                .type(MaterialType.PDF)
                                .moduleId(moduleId)
                                .fileUrl("https://cdn.example.com/doc.pdf")
                                .build()))
                        .build());

        mockMvc.perform(get("/api/v1/course/{courseId}/modules/{moduleId}/learning-materials", courseId, moduleId)
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Learning materials fetched successfully"));

        verify(learningMaterialService).getLearningMaterials(courseId, moduleId, AUTH_HEADER);
    }

    @Test
    void getLearningMaterial_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();

        when(learningMaterialService.getLearningMaterial(courseId, moduleId, materialId, AUTH_HEADER))
                .thenReturn(ApiResponse.<LearningMaterialResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Learning material fetched successfully")
                        .data(LearningMaterialResponse.builder()
                                .id(materialId)
                                .title("Doc")
                                .type(MaterialType.PDF)
                                .moduleId(moduleId)
                                .fileUrl("https://cdn.example.com/doc.pdf")
                                .build())
                        .build());

        mockMvc.perform(get("/api/v1/course/{courseId}/modules/{moduleId}/learning-materials/{materialId}", courseId, moduleId, materialId)
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Learning material fetched successfully"));

        verify(learningMaterialService).getLearningMaterial(courseId, moduleId, materialId, AUTH_HEADER);
    }

    @Test
    void deleteModule_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        when(moduleService.deleteModule(courseId, moduleId)).thenReturn(
                ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message("Module deleted successfully")
                        .build()
        );

        mockMvc.perform(delete("/api/v1/course/{courseId}/modules/{moduleId}", courseId, moduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Module deleted successfully"));

        verify(moduleService).deleteModule(courseId, moduleId);
    }

    @Test
    void deleteLesson_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        when(lessonService.deleteLesson(courseId, moduleId, lessonId)).thenReturn(
                ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message("Lesson deleted successfully")
                        .build()
        );

        mockMvc.perform(delete("/api/v1/course/{courseId}/modules/{moduleId}/lessons/{lessonId}", courseId, moduleId, lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lesson deleted successfully"));

        verify(lessonService).deleteLesson(courseId, moduleId, lessonId);
    }

    @Test
    void deleteLearningMaterial_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();

        when(learningMaterialService.deleteLearningMaterial(courseId, moduleId, materialId)).thenReturn(
                ApiResponse.builder()
                        .status(HttpStatus.OK)
                        .message("Learning material deleted successfully")
                        .build()
        );

        mockMvc.perform(delete("/api/v1/course/{courseId}/modules/{moduleId}/learning-materials/{materialId}", courseId, moduleId, materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Learning material deleted successfully"));

        verify(learningMaterialService).deleteLearningMaterial(courseId, moduleId, materialId);
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
        assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
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
        assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
    }

    @Test
    void createLearningMaterial_hasTeacherPreAuthorize() throws NoSuchMethodException {
        Method method = CourseContentController.class.getDeclaredMethod(
                "createLearningMaterial",
                UUID.class,
                UUID.class,
                LearningMaterialCreateRequest.class,
                String.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
    }

        @Test
        void updateModule_hasTeacherPreAuthorize() throws NoSuchMethodException {
                Method method = CourseContentController.class.getDeclaredMethod(
                                "updateModule",
                                UUID.class,
                                UUID.class,
                                ModuleCreateRequest.class
                );

                PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

                assertThat(preAuthorize).isNotNull();
                assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
        }

        @Test
        void updateLesson_hasTeacherPreAuthorize() throws NoSuchMethodException {
                Method method = CourseContentController.class.getDeclaredMethod(
                                "updateLesson",
                                UUID.class,
                                UUID.class,
                                UUID.class,
                                LessonCreateRequest.class
                );

                PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

                assertThat(preAuthorize).isNotNull();
                assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
        }

        @Test
        void updateLearningMaterial_hasTeacherPreAuthorize() throws NoSuchMethodException {
                Method method = CourseContentController.class.getDeclaredMethod(
                                "updateLearningMaterial",
                                UUID.class,
                                UUID.class,
                                UUID.class,
                                LearningMaterialCreateRequest.class,
                                String.class
                );

                PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

                assertThat(preAuthorize).isNotNull();
                assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
        }

    @Test
    void deleteModule_hasTeacherPreAuthorize() throws NoSuchMethodException {
        Method method = CourseContentController.class.getDeclaredMethod(
                "deleteModule",
                UUID.class,
                UUID.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
    }

    @Test
    void deleteLesson_hasTeacherPreAuthorize() throws NoSuchMethodException {
        Method method = CourseContentController.class.getDeclaredMethod(
                "deleteLesson",
                UUID.class,
                UUID.class,
                UUID.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
    }

    @Test
    void deleteLearningMaterial_hasTeacherPreAuthorize() throws NoSuchMethodException {
        Method method = CourseContentController.class.getDeclaredMethod(
                "deleteLearningMaterial",
                UUID.class,
                UUID.class,
                UUID.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
    }

        @Test
        void getModules_hasTeacherPreAuthorize() throws NoSuchMethodException {
                Method method = CourseContentController.class.getDeclaredMethod("getModules", UUID.class);
                PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
                assertThat(preAuthorize).isNotNull();
                assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
        }

        @Test
        void getModule_hasTeacherPreAuthorize() throws NoSuchMethodException {
                Method method = CourseContentController.class.getDeclaredMethod("getModule", UUID.class, UUID.class);
                PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
                assertThat(preAuthorize).isNotNull();
                assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
        }

        @Test
        void getLessons_hasTeacherPreAuthorize() throws NoSuchMethodException {
                Method method = CourseContentController.class.getDeclaredMethod("getLessons", UUID.class, UUID.class);
                PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
                assertThat(preAuthorize).isNotNull();
                assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
        }

        @Test
        void getLesson_hasTeacherPreAuthorize() throws NoSuchMethodException {
                Method method = CourseContentController.class.getDeclaredMethod("getLesson", UUID.class, UUID.class, UUID.class);
                PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
                assertThat(preAuthorize).isNotNull();
                assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_ROLES);
        }

        @Test
        void getLearningMaterials_hasTeacherPreAuthorize() throws NoSuchMethodException {
                Method method = CourseContentController.class.getDeclaredMethod(
                                "getLearningMaterials",
                                UUID.class,
                                UUID.class,
                                String.class
                );
                PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
                assertThat(preAuthorize).isNotNull();
                assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_READ_ROLES);
        }

        @Test
        void getLearningMaterial_hasTeacherPreAuthorize() throws NoSuchMethodException {
                Method method = CourseContentController.class.getDeclaredMethod(
                                "getLearningMaterial",
                                UUID.class,
                                UUID.class,
                                UUID.class,
                                String.class
                );
                PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
                assertThat(preAuthorize).isNotNull();
                assertThat(preAuthorize.value()).isEqualTo(COURSE_CONTENT_READ_ROLES);
        }
}
