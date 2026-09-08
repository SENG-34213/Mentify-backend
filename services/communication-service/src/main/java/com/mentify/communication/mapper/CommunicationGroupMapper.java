package com.mentify.communication.mapper;

import com.mentify.communication.dto.response.CommunicationGroupResponse;
import com.mentify.communication.dto.response.GroupMemberResponse;
import com.mentify.communication.entity.CommunicationGroup;
import com.mentify.communication.entity.GroupMember;

public final class CommunicationGroupMapper {

    private CommunicationGroupMapper() {
    }

    public static CommunicationGroupResponse toGroupResponse(CommunicationGroup group) {
        return CommunicationGroupResponse.builder()
                .id(group.getId())
                .courseId(group.getCourseId())
                .name(group.getName())
                .description(group.getDescription())
                .status(group.getStatus())
                .build();
    }

    public static GroupMemberResponse toMemberResponse(GroupMember member) {
        return GroupMemberResponse.builder()
                .userId(member.getUserId())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
