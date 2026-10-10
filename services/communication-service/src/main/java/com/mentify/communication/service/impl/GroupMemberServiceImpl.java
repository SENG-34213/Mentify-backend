package com.mentify.communication.service.impl;

import com.mentify.communication.dto.response.GroupMemberResponse;
import com.mentify.communication.entity.CommunicationGroup;
import com.mentify.communication.entity.GroupMember;
import com.mentify.communication.enums.GroupMemberRole;
import com.mentify.communication.exception.DuplicateGroupMemberException;
import com.mentify.communication.exception.UnauthorizedGroupAccessException;
import com.mentify.communication.mapper.CommunicationGroupMapper;
import com.mentify.communication.repository.GroupMemberRepository;
import com.mentify.communication.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional
    public GroupMember addGroupMember(CommunicationGroup group, UUID userId, GroupMemberRole role) {
        if (groupMemberRepository.existsByGroup_IdAndUserIdAndIsActiveTrue(group.getId(), userId)) {
            throw new DuplicateGroupMemberException(group.getId(), userId);
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .userId(userId)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .build();
        member.setActive(true);

        return groupMemberRepository.save(member);
    }

    @Override
    @Transactional
    public List<GroupMember> addGroupMembers(CommunicationGroup group, Collection<UUID> userIds, GroupMemberRole role) {
        LinkedHashSet<UUID> uniqueUserIds = new LinkedHashSet<>(userIds);

        return uniqueUserIds.stream()
                .filter(userId -> !groupMemberRepository.existsByGroup_IdAndUserIdAndIsActiveTrue(group.getId(), userId))
                .map(userId -> {
                    GroupMember member = GroupMember.builder()
                            .group(group)
                            .userId(userId)
                            .role(role)
                            .joinedAt(LocalDateTime.now())
                            .build();
                    member.setActive(true);
                    return member;
                })
                .map(groupMemberRepository::save)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GroupMember validateActiveMembership(UUID groupId, UUID userId) {
        return groupMemberRepository.findByGroup_IdAndUserIdAndIsActiveTrue(groupId, userId)
                .orElseThrow(() -> new UnauthorizedGroupAccessException("You are not an active member of this communication group"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveMember(UUID groupId, UUID userId) {
        return groupMemberRepository.existsByGroup_IdAndUserIdAndIsActiveTrue(groupId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getActiveGroupMembers(UUID groupId, UUID currentUserId) {
        validateActiveMembership(groupId, currentUserId);

        return groupMemberRepository.findByGroup_IdAndIsActiveTrue(groupId).stream()
                .map(CommunicationGroupMapper::toMemberResponse)
                .toList();
    }
}
