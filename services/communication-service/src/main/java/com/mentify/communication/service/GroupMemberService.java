package com.mentify.communication.service;

import com.mentify.communication.dto.response.GroupMemberResponse;
import com.mentify.communication.entity.CommunicationGroup;
import com.mentify.communication.entity.GroupMember;
import com.mentify.communication.enums.GroupMemberRole;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface GroupMemberService {

    GroupMember addGroupMember(CommunicationGroup group, UUID userId, GroupMemberRole role);

    List<GroupMember> addGroupMembers(CommunicationGroup group, Collection<UUID> userIds, GroupMemberRole role);

    GroupMember validateActiveMembership(UUID groupId, UUID userId);

    boolean isActiveMember(UUID groupId, UUID userId);

    List<GroupMemberResponse> getActiveGroupMembers(UUID groupId, UUID currentUserId);
}
