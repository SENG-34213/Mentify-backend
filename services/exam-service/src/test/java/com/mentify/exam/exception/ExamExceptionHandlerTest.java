package com.mentify.exam.exception;

import com.mentify.exam.support.SecurityProbeController;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {com.mentify.ExamServiceApplication.class, SecurityProbeController.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExamExceptionHandlerTest {

    private static final String BASE = "/api/v1/exams/probe";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    private String bearer() {
        Jwt jwt = Jwt.withTokenValue("ok").header("alg", "none").subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        when(jwtDecoder.decode("ok")).thenReturn(jwt);
        return "Bearer ok";
    }

    @Test
    void unexpectedErrorsReturnSafeStructuredResponse() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE + "/boom").header("Authorization", bearer()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.message").value(ExamExceptionHandler.INTERNAL_ERROR_MESSAGE))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        for (String leak : new String[]{"jdbc:", "password", "super-secret", "org.hibernate", "Exception", "\tat "}) {
            assertFalse(body.contains(leak), "Response leaked: " + leak);
        }
    }

    @Test
    void accessDeniedReturns403() throws Exception {
        mockMvc.perform(get(BASE + "/denied").header("Authorization", bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void validationErrorReturns400() throws Exception {
        mockMvc.perform(post(BASE + "/validate").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("name must not be blank"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post(BASE + "/validate").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }
}
