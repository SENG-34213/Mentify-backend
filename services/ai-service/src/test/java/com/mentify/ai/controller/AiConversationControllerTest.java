package com.mentify.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.ai.config.SecurityConfig;
import com.mentify.ai.dto.request.AiConversationCreateRequest;
import com.mentify.ai.dto.request.AiConversationUpdateRequest;
import com.mentify.ai.dto.response.AiConversationDetailResponse;
import com.mentify.ai.dto.response.AiConversationResponse;
import com.mentify.ai.dto.response.AiMessageResponse;
import com.mentify.ai.enums.AiMessageRole;
import com.mentify.ai.service.AiConversationService;
import com.mentify.common.security.KeycloakJwtAuthenticationConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiConversationController.class, properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AiConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockBean
    private AiConversationService conversationService;

    @Test
    void conversationEndpointsShouldBeProtected() throws Exception {
        mockMvc.perform(get("/api/ai/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void conversationEndpointsShouldRejectStudents() throws Exception {
        mockMvc.perform(get("/api/ai/conversations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createConversationShouldReturnCreatedResponse() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(conversationService.createConversation(any())).thenReturn(AiConversationResponse.builder()
                .id(conversationId)
                .title("Planning")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/api/ai/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AiConversationCreateRequest.builder()
                                .title("Planning")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.data.title").value("Planning"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getConversationsShouldReturnCurrentUserConversations() throws Exception {
        when(conversationService.getCurrentUserConversations()).thenReturn(List.of(
                AiConversationResponse.builder().id(UUID.randomUUID()).title("Recent").build(),
                AiConversationResponse.builder().id(UUID.randomUUID()).title("Older").build()
        ));

        mockMvc.perform(get("/api/ai/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].title").value("Recent"))
                .andExpect(jsonPath("$.data[1].title").value("Older"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getConversationShouldReturnMessages() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(conversationService.getConversation(conversationId)).thenReturn(AiConversationDetailResponse.builder()
                .id(conversationId)
                .title("Conversation")
                .messages(List.of(
                        AiMessageResponse.builder().role(AiMessageRole.USER).content("Hi").build(),
                        AiMessageResponse.builder().role(AiMessageRole.ASSISTANT).content("Hello").build()
                ))
                .build());

        mockMvc.perform(get("/api/ai/conversations/{conversationId}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.data.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.data.messages[1].role").value("ASSISTANT"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateConversationShouldValidateTitle() throws Exception {
        UUID conversationId = UUID.randomUUID();

        mockMvc.perform(put("/api/ai/conversations/{conversationId}", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AiConversationUpdateRequest.builder()
                                .title("")
                                .build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateConversationShouldReturnUpdatedConversation() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(conversationService.updateConversation(eq(conversationId), any())).thenReturn(AiConversationResponse.builder()
                .id(conversationId)
                .title("Renamed")
                .build());

        mockMvc.perform(put("/api/ai/conversations/{conversationId}", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AiConversationUpdateRequest.builder()
                                .title("Renamed")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Renamed"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteConversationShouldReturnSuccess() throws Exception {
        UUID conversationId = UUID.randomUUID();
        doNothing().when(conversationService).deleteConversation(conversationId);

        mockMvc.perform(delete("/api/ai/conversations/{conversationId}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("AI conversation deleted successfully"));
    }
}
