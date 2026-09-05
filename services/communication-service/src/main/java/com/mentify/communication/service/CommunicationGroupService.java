package com.mentify.communication.service;

import com.mentify.communication.dto.request.CreateCommunicationGroupRequest;
import com.mentify.communication.dto.response.CommunicationGroupResponse;

import java.util.List;
import java.util.UUID;

public interface CommunicationGroupService {

    CommunicationGroupResponse createGroup(CreateCommunicationGroupRequest request, String authorizationHeader);

    List<CommunicationGroupResponse> getCurrentUserGroups();

    CommunicationGroupResponse getGroupById(UUID groupId);

    CommunicationGroupResponse archiveGroup(UUID groupId);
}
