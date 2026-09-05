package com.mentify.communication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.communication.dto.request.CreateCommunicationGroupRequest;
import com.mentify.communication.dto.response.CommunicationGroupResponse;
import com.mentify.communication.dto.response.GroupMemberResponse;
import com.mentify.communication.enums.GroupMemberRole;
import com.mentify.communication.enums.GroupStatus;
import com.mentify.communication.security.AuthenticatedUserService;
import com.mentify.communication.service.CommunicationGroupService;
import com.mentify.communication.service.GroupMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CommunicationGroupController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class CommunicationGroupControllerTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommunicationGroupService communicationGroupService;

    @MockBean
    private GroupMemberService groupMemberService;

    @MockBean
    private AuthenticatedUserService authenticatedUserService;

    @Test
    void createGroupReturnsCreatedResponse() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(communicationGroupService.createGroup(any(CreateCommunicationGroupRequest.class), eq(AUTH_HEADER)))
                .thenReturn(groupResponse(groupId, courseId, GroupStatus.ACTIVE));

        mockMvc.perform(post("/api/communication/groups")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(courseId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Communication group created successfully"))
                .andExpect(jsonPath("$.data.id").value(groupId.toString()))
                .andExpect(jsonPath("$.data.courseId").value(courseId.toString()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(communicationGroupService).createGroup(any(CreateCommunicationGroupRequest.class), eq(AUTH_HEADER));
    }

    @Test
    void createGroupRejectsInvalidRequest() throws Exception {
        CreateCommunicationGroupRequest request = validRequest(null);

        mockMvc.perform(post("/api/communication/groups")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(communicationGroupService);
    }

    @Test
    void getCurrentUserGroupsReturnsGroups() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(communicationGroupService.getCurrentUserGroups())
                .thenReturn(List.of(groupResponse(groupId, courseId, GroupStatus.ACTIVE)));

        mockMvc.perform(get("/api/communication/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(groupId.toString()));
    }

    @Test
    void getGroupMembersReturnsMembers() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(groupMemberService.getActiveGroupMembers(groupId, userId))
                .thenReturn(List.of(GroupMemberResponse.builder()
                        .userId(userId)
                        .role(GroupMemberRole.STUDENT)
                        .build()));

        mockMvc.perform(get("/api/communication/groups/{groupId}/members", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.data[0].role").value("STUDENT"));
    }

    @Test
    void createAndArchiveEndpointsAreRoleRestricted() throws NoSuchMethodException {
        Method create = CommunicationGroupController.class.getDeclaredMethod(
                "createGroup",
                CreateCommunicationGroupRequest.class,
                String.class
        );
        Method archive = CommunicationGroupController.class.getDeclaredMethod("archiveGroup", UUID.class);

        assertThat(create.getAnnotation(PreAuthorize.class).value()).contains("ADMIN", "SUPER_ADMIN", "TEACHER");
        assertThat(archive.getAnnotation(PreAuthorize.class).value()).contains("ADMIN", "SUPER_ADMIN", "TEACHER");
    }

    private CreateCommunicationGroupRequest validRequest(UUID courseId) {
        return CreateCommunicationGroupRequest.builder()
                .courseId(courseId)
                .name("Java Programming Discussion")
                .description("Official communication group")
                .build();
    }

    private CommunicationGroupResponse groupResponse(UUID groupId, UUID courseId, GroupStatus status) {
        return CommunicationGroupResponse.builder()
                .id(groupId)
                .courseId(courseId)
                .name("Java Programming Discussion")
                .description("Official communication group")
                .status(status)
                .build();
    }
}
