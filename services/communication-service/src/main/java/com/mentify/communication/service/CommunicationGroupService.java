package com.mentify.communication.service;

import com.mentify.communication.dto.request.CreateCommunicationGroupRequest;
import com.mentify.communication.dto.request.UpdateCommunicationGroupRequest;
import com.mentify.communication.dto.response.CommunicationGroupResponse;
import com.mentify.communication.dto.response.GroupMemberResponse;

import java.util.List;
import java.util.UUID;

public interface CommunicationGroupService {

    CommunicationGroupResponse createGroup(CreateCommunicationGroupRequest request, String authorizationHeader);

    List<CommunicationGroupResponse> getCurrentUserGroups();

    CommunicationGroupResponse getGroupById(UUID groupId);

    CommunicationGroupResponse updateGroup(UUID groupId, UpdateCommunicationGroupRequest request);

    GroupMemberResponse addStudentToGroup(UUID groupId, UUID studentId);

    GroupMemberResponse addStudentToCourseGroup(UUID courseId, UUID studentId);

    CommunicationGroupResponse archiveGroup(UUID groupId);

    CommunicationGroupResponse unarchiveGroup(UUID groupId);
}
