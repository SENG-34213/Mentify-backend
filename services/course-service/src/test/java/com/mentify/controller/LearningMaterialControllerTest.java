package com.mentify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.dto.LearningMaterialRequest;
import com.mentify.dto.LearningMaterialResponse;
import com.mentify.enums.MaterialType;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.LearningMaterialService;
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

@WebMvcTest(controllers = LearningMaterialController.class, properties = "spring.cloud.config.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
class LearningMaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LearningMaterialService learningMaterialService;

    @Test
    void createLearningMaterial_whenValidRequest_returnsCreated() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        LearningMaterialResponse responseData = LearningMaterialResponse.builder()
                .id(UUID.randomUUID())
                .title("Video 1")
                .type(MaterialType.VIDEO)
                .build();

        when(learningMaterialService.createLearningMaterial(eq(courseId), eq(moduleId), any(LearningMaterialRequest.class))).thenReturn(
                ApiResponse.<LearningMaterialResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Learning material created successfully")
                        .data(responseData)
                        .build()
        );

        LearningMaterialRequest request = LearningMaterialRequest.builder()
                .title("Video 1")
                .type(MaterialType.VIDEO)
                .fileUrl("https://cdn.example.com/video.mp4")
                .build();

        mockMvc.perform(post("/api/v1/course/{courseId}/modules/{moduleId}/learning-materials", courseId, moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Learning material created successfully"));
    }

    @Test
    void updateLearningMaterial_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        LearningMaterialResponse responseData = LearningMaterialResponse.builder()
                .id(materialId)
                .title("Video 2")
                .type(MaterialType.PDF)
                .build();

        when(learningMaterialService.updateLearningMaterial(eq(courseId), eq(moduleId), eq(materialId), any(LearningMaterialRequest.class)))
                .thenReturn(ApiResponse.<LearningMaterialResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Learning material updated successfully")
                        .data(responseData)
                        .build());

        LearningMaterialRequest request = LearningMaterialRequest.builder()
                .title("Video 2")
                .type(MaterialType.PDF)
                .fileUrl("https://cdn.example.com/video.pdf")
                .build();

        mockMvc.perform(put("/api/v1/course/{courseId}/modules/{moduleId}/learning-materials/{materialId}", courseId, moduleId, materialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Learning material updated successfully"));
    }

    @Test
    void deleteLearningMaterial_whenExists_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();

        when(learningMaterialService.deleteLearningMaterial(courseId, moduleId, materialId)).thenReturn(
                ApiResponse.builder().status(HttpStatus.OK).message("Learning material deleted successfully").build()
        );

        mockMvc.perform(delete("/api/v1/course/{courseId}/modules/{moduleId}/learning-materials/{materialId}", courseId, moduleId, materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Learning material deleted successfully"));

        verify(learningMaterialService).deleteLearningMaterial(courseId, moduleId, materialId);
    }

    @Test
    void createLearningMaterial_whenTitleBlank_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        LearningMaterialRequest request = LearningMaterialRequest.builder()
                .title(" ")
                .type(MaterialType.VIDEO)
                .fileUrl("https://cdn.example.com/video.mp4")
                .build();

        mockMvc.perform(post("/api/v1/course/{courseId}/modules/{moduleId}/learning-materials", courseId, moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(learningMaterialService);
    }

    @Test
    void endpoints_haveTeacherPreAuthorize() throws NoSuchMethodException {
        Method createMethod = LearningMaterialController.class.getDeclaredMethod(
                "createLearningMaterial", UUID.class, UUID.class, LearningMaterialRequest.class);
        Method updateMethod = LearningMaterialController.class.getDeclaredMethod(
                "updateLearningMaterial", UUID.class, UUID.class, UUID.class, LearningMaterialRequest.class);
        Method deleteMethod = LearningMaterialController.class.getDeclaredMethod(
                "deleteLearningMaterial", UUID.class, UUID.class, UUID.class);

        assertThat(createMethod.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('TEACHER')");
        assertThat(updateMethod.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('TEACHER')");
        assertThat(deleteMethod.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('TEACHER')");
    }
}
