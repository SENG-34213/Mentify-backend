package com.mentify.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CalendarFoundationIntegrationTest.SecurityProbeConfiguration.class)
class CalendarFoundationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void givenNoToken_whenProtectedResourceIsRequested_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/test/calendar/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenStudentRole_whenAdminResourceIsRequested_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/test/calendar/protected")
                        .with(jwt().authorities(AuthorityUtils.createAuthorityList("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAdminRole_whenAdminResourceIsRequested_thenReturnsOk() throws Exception {
        mockMvc.perform(get("/test/calendar/protected")
                        .with(jwt().authorities(AuthorityUtils.createAuthorityList("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void whenHealthIsRequested_thenDatabaseBackedHealthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void whenReadinessIsRequested_thenDatabaseReadinessIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void givenAdminRole_whenEnvironmentEndpointIsRequested_thenOperationalDetailsAreNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/env")
                        .with(jwt().authorities(AuthorityUtils.createAuthorityList("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityProbeConfiguration {

        @Bean
        SecurityProbeController securityProbeController() {
            return new SecurityProbeController();
        }
    }

    @Controller
    static class SecurityProbeController {

        @GetMapping("/test/calendar/protected")
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        @ResponseBody
        String protectedResource() {
            return "ok";
        }
    }
}
