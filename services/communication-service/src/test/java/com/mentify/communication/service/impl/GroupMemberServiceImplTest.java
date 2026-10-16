package com.mentify.communication.service.impl;

import com.mentify.communication.dto.response.GroupMemberResponse;
import com.mentify.communication.entity.CommunicationGroup;
import com.mentify.communication.entity.GroupMember;
import com.mentify.communication.enums.GroupMemberRole;
import com.mentify.communication.enums.GroupStatus;
import com.mentify.communication.exception.DuplicateGroupMemberException;
import com.mentify.communication.exception.UnauthorizedGroupAccessException;
import com.mentify.communication.repository.GroupMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupMemberServiceImplTest {

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private GroupMemberServiceImpl groupMemberService;

    @Test
    void creatorIsAddedAsMember() {
        UUID userId = UUID.randomUUID();
        CommunicationGroup group = group(UUID.randomUUID());

        when(groupMemberRepository.existsByGroup_IdAndUserIdAndIsActiveTrue(group.getId(), userId)).thenReturn(false);
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupMember member = groupMemberService.addGroupMember(group, userId, GroupMemberRole.ADMIN);

        assertEquals(group, member.getGroup());
        assertEquals(userId, member.getUserId());
        assertEquals(GroupMemberRole.ADMIN, member.getRole());
    }

    @Test
    void duplicateMemberIsPrevented() {
        UUID userId = UUID.randomUUID();
        CommunicationGroup group = group(UUID.randomUUID());

        when(groupMemberRepository.existsByGroup_IdAndUserIdAndIsActiveTrue(group.getId(), userId)).thenReturn(true);

        assertThrows(
                DuplicateGroupMemberException.class,
                () -> groupMemberService.addGroupMember(group, userId, GroupMemberRole.STUDENT)
        );
    }

    @Test
    void enrolledStudentsAreAddedOnce() {
        UUID groupId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        CommunicationGroup group = group(groupId);

        when(groupMemberRepository.existsByGroup_IdAndUserIdAndIsActiveTrue(groupId, studentId)).thenReturn(false);
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<GroupMember> members = groupMemberService.addGroupMembers(
                group,
                List.of(studentId, studentId),
                GroupMemberRole.STUDENT
        );

        assertEquals(1, members.size());
        assertEquals(studentId, members.get(0).getUserId());
        assertEquals(GroupMemberRole.STUDENT, members.get(0).getRole());
    }

    @Test
    void memberCanRetrieveGroupMembers() {
        UUID groupId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        CommunicationGroup group = group(groupId);

        when(groupMemberRepository.findByGroup_IdAndUserIdAndIsActiveTrue(groupId, studentId))
                .thenReturn(Optional.of(member(group, studentId, GroupMemberRole.STUDENT)));
        when(groupMemberRepository.findByGroup_IdAndIsActiveTrue(groupId))
                .thenReturn(List.of(
                        member(group, teacherId, GroupMemberRole.TEACHER),
                        member(group, studentId, GroupMemberRole.STUDENT)
                ));

        List<GroupMemberResponse> response = groupMemberService.getActiveGroupMembers(groupId, studentId);

        assertEquals(2, response.size());
        assertEquals(GroupMemberRole.TEACHER, response.get(0).getRole());
        assertEquals(GroupMemberRole.STUDENT, response.get(1).getRole());
    }

    @Test
    void nonMemberCannotRetrieveGroupMembers() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(groupMemberRepository.findByGroup_IdAndUserIdAndIsActiveTrue(groupId, userId)).thenReturn(Optional.empty());

        assertThrows(
                UnauthorizedGroupAccessException.class,
                () -> groupMemberService.getActiveGroupMembers(groupId, userId)
        );
    }

    private CommunicationGroup group(UUID groupId) {
        CommunicationGroup group = CommunicationGroup.builder()
                .courseId(UUID.randomUUID())
                .name("Java Programming Discussion")
                .status(GroupStatus.ACTIVE)
                .build();
        group.setId(groupId);
        return group;
    }

    private GroupMember member(CommunicationGroup group, UUID userId, GroupMemberRole role) {
        GroupMember member = GroupMember.builder()
                .group(group)
                .userId(userId)
                .role(role)
                .build();
        member.setActive(true);
        return member;
    }
}
