package com.mentify.ai.controller;
 
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.ai.dto.request.AiGenerateRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
import com.mentify.ai.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
 
import java.time.LocalDateTime;
 
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
 
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiGenerationControllerTest {
 
    @Autowired
    private MockMvc mockMvc;
 
    @MockBean
    private AiService aiService;
 
    @Autowired
    private ObjectMapper objectMapper;
 
    @Test
    @WithMockUser
    void generate_ShouldReturnSuccess_WhenAuthenticatedAndRequestIsValid() throws Exception {
        // Arrange
        AiGenerateRequest request = new AiGenerateRequest("Explain inheritance");
        AiGenerateResponse response = AiGenerateResponse.builder()
                .content("Inheritance is...")
                .provider("OPENAI")
                .model("gpt-4o")
                .generatedAt(LocalDateTime.now())
                .build();
 
        when(aiService.generate(any(AiGenerateRequest.class))).thenReturn(response);
 
        // Act & Assert
        mockMvc.perform(post("/api/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.content").value("Inheritance is..."))
                .andExpect(jsonPath("$.data.provider").value("OPENAI"));
    }
 
    @Test
    void generate_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        // Arrange
        AiGenerateRequest request = new AiGenerateRequest("Explain inheritance");
 
        // Act & Assert
        mockMvc.perform(post("/api/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
 
    @Test
    @WithMockUser
    void generate_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // Arrange
        AiGenerateRequest request = new AiGenerateRequest(""); // Blank prompt
 
        // Act & Assert
        mockMvc.perform(post("/api/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
