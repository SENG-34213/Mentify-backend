package com.mentify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.dto.ModuleRequest;
import com.mentify.dto.ModuleResponse;
import com.mentify.payload.response.ApiResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ModuleController.class, properties = "spring.cloud.config.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
class ModuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ModuleService moduleService;

    @Test
    void createModule_whenValidRequest_returnsCreated() throws Exception {
        UUID courseId = UUID.randomUUID();
        ModuleResponse responseData = ModuleResponse.builder().id(UUID.randomUUID()).title("Algebra").build();

        when(moduleService.createModule(eq(courseId), any(ModuleRequest.class))).thenReturn(
                ApiResponse.<ModuleResponse>builder()
                        .status(HttpStatus.CREATED)
                        .message("Module created successfully")
                        .data(responseData)
                        .build()
        );

        ModuleRequest request = ModuleRequest.builder().title("Algebra").sequenceOrder(1).build();

        mockMvc.perform(post("/api/v1/course/{courseId}/modules", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Module created successfully"));
    }

    @Test
    void updateModule_whenValidRequest_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        ModuleResponse responseData = ModuleResponse.builder().id(moduleId).title("Algebra 2").build();

        when(moduleService.updateModule(eq(courseId), eq(moduleId), any(ModuleRequest.class))).thenReturn(
                ApiResponse.<ModuleResponse>builder()
                        .status(HttpStatus.OK)
                        .message("Module updated successfully")
                        .data(responseData)
                        .build()
        );

        ModuleRequest request = ModuleRequest.builder().title("Algebra 2").sequenceOrder(2).build();

        mockMvc.perform(put("/api/v1/course/{courseId}/modules/{moduleId}", courseId, moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Module updated successfully"));
    }

    @Test
    void deleteModule_whenExists_returnsOk() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        when(moduleService.deleteModule(courseId, moduleId)).thenReturn(
                ApiResponse.builder().status(HttpStatus.OK).message("Module deleted successfully").build()
        );

        mockMvc.perform(delete("/api/v1/course/{courseId}/modules/{moduleId}", courseId, moduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Module deleted successfully"));

        verify(moduleService).deleteModule(courseId, moduleId);
    }

    @Test
    void createModule_whenTitleBlank_returnsBadRequest() throws Exception {
        UUID courseId = UUID.randomUUID();
        ModuleRequest request = ModuleRequest.builder().title(" ").sequenceOrder(1).build();

        mockMvc.perform(post("/api/v1/course/{courseId}/modules", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(moduleService);
    }

    @Test
    void endpoints_haveTeacherPreAuthorize() throws NoSuchMethodException {
        Method createMethod = ModuleController.class.getDeclaredMethod("createModule", UUID.class, ModuleRequest.class);
        Method updateMethod = ModuleController.class.getDeclaredMethod("updateModule", UUID.class, UUID.class, ModuleRequest.class);
        Method deleteMethod = ModuleController.class.getDeclaredMethod("deleteModule", UUID.class, UUID.class);

        assertThat(createMethod.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('TEACHER')");
        assertThat(updateMethod.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('TEACHER')");
        assertThat(deleteMethod.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('TEACHER')");
    }
}
