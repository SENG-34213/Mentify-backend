package com.mentify.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.userservice.config.SecurityConfig;
import com.mentify.userservice.dto.UpdateUserStatusRequest;
import com.mentify.userservice.exception.GlobalExceptionHandler;
import com.mentify.userservice.exception.UserNotFoundException;
import com.mentify.userservice.repository.UserRepository;
import com.mentify.userservice.security.JwtAuthenticationFilter;
import com.mentify.userservice.security.JwtService;
import com.mentify.userservice.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationFilter.class})
@DisplayName("AdminController Unit Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    private static final String UPDATE_STATUS_URL = "/api/v1/admin/users/{userId}/status";

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/status - should return 200 when admin deactivates user")
    @WithMockUser(roles = "ADMIN")
    void updateUserStatus_ShouldReturn200_WhenAdminDeactivatesUser() throws Exception {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);
        doNothing().when(adminService).updateUserStatus(1L, false);

        mockMvc.perform(patch(UPDATE_STATUS_URL, 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User status updated successfully"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/status - should return 200 when admin activates user")
    @WithMockUser(roles = "ADMIN")
    void updateUserStatus_ShouldReturn200_WhenAdminActivatesUser() throws Exception {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(true);
        doNothing().when(adminService).updateUserStatus(2L, true);

        mockMvc.perform(patch(UPDATE_STATUS_URL, 2L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User status updated successfully"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/status - should return 404 when user not found")
    @WithMockUser(roles = "ADMIN")
    void updateUserStatus_ShouldReturn404_WhenUserNotFound() throws Exception {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);
        doThrow(new UserNotFoundException(99L)).when(adminService).updateUserStatus(99L, false);

        mockMvc.perform(patch(UPDATE_STATUS_URL, 99L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/status - should return 403 when non-admin user attempts update")
    @WithMockUser(roles = "USER")
    void updateUserStatus_ShouldReturn403_WhenNonAdminUser() throws Exception {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);

        mockMvc.perform(patch(UPDATE_STATUS_URL, 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/status - should return 401 when unauthenticated")
    void updateUserStatus_ShouldReturn401_WhenUnauthenticated() throws Exception {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);

        mockMvc.perform(patch(UPDATE_STATUS_URL, 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/status - should return 400 when active field is missing")
    @WithMockUser(roles = "ADMIN")
    void updateUserStatus_ShouldReturn400_WhenActiveFieldMissing() throws Exception {
        mockMvc.perform(patch(UPDATE_STATUS_URL, 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
