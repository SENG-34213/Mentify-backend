package com.mentify.communication.service.impl;

import com.mentify.communication.client.CourseServiceClient;
import com.mentify.communication.client.EntrollmentServiceClient;
import com.mentify.communication.client.dto.CourseLookupResponse;
import com.mentify.communication.dto.request.CreateCommunicationGroupRequest;
import com.mentify.communication.dto.response.CommunicationGroupResponse;
import com.mentify.communication.entity.CommunicationGroup;
import com.mentify.communication.entity.GroupMember;
import com.mentify.communication.enums.GroupMemberRole;
import com.mentify.communication.enums.GroupStatus;
import com.mentify.communication.exception.CourseNotFoundException;
import com.mentify.communication.exception.DuplicateCommunicationGroupException;
import com.mentify.communication.exception.TeacherNotAssignedToCourseException;
import com.mentify.communication.exception.UnauthorizedGroupAccessException;
import com.mentify.communication.repository.CommunicationGroupRepository;
import com.mentify.communication.repository.GroupMemberRepository;
import com.mentify.communication.security.AuthenticatedUserService;
import com.mentify.communication.service.GroupMemberService;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunicationGroupServiceImplTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private CommunicationGroupRepository communicationGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private EntrollmentServiceClient entrollmentServiceClient;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private CommunicationGroupServiceImpl communicationGroupService;

    @Test
    void adminCanCreateGroupAndMembersAreSeeded() {
        UUID adminId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(adminId);
        when(authenticatedUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_ADMIN"));
        when(courseServiceClient.lookupCourseById(courseId, AUTH_HEADER)).thenReturn(successCourse(courseId, teacherId));
        when(communicationGroupRepository.existsByCourseId(courseId)).thenReturn(false);
        when(communicationGroupRepository.save(any(CommunicationGroup.class))).thenAnswer(invocation -> {
            CommunicationGroup group = invocation.getArgument(0);
            group.setId(UUID.randomUUID());
            return group;
        });
        when(entrollmentServiceClient.getEnrolledStudentIdsByCourse(courseId, AUTH_HEADER))
                .thenReturn(successStudents(List.of(studentId)));

        CommunicationGroupResponse response = communicationGroupService.createGroup(validRequest(courseId), AUTH_HEADER);

        assertEquals(courseId, response.getCourseId());
        assertEquals(GroupStatus.ACTIVE, response.getStatus());
        verify(groupMemberService).addGroupMember(any(CommunicationGroup.class), eq(adminId), eq(GroupMemberRole.ADMIN));
        verify(groupMemberService).addGroupMembers(any(CommunicationGroup.class), eq(List.of(teacherId)), eq(GroupMemberRole.TEACHER));
        verify(groupMemberService).addGroupMembers(any(CommunicationGroup.class), eq(List.of(studentId)), eq(GroupMemberRole.STUDENT));
    }

    @Test
    void teacherCanCreateGroupForAssignedCourse() {
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(authenticatedUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TEACHER"));
        when(courseServiceClient.lookupCourseById(courseId, AUTH_HEADER)).thenReturn(successCourse(courseId, teacherId));
        when(communicationGroupRepository.existsByCourseId(courseId)).thenReturn(false);
        when(communicationGroupRepository.save(any(CommunicationGroup.class))).thenAnswer(invocation -> {
            CommunicationGroup group = invocation.getArgument(0);
            group.setId(UUID.randomUUID());
            return group;
        });
        when(entrollmentServiceClient.getEnrolledStudentIdsByCourse(courseId, AUTH_HEADER))
                .thenReturn(successStudents(List.of()));

        communicationGroupService.createGroup(validRequest(courseId), AUTH_HEADER);

        verify(groupMemberService).addGroupMember(any(CommunicationGroup.class), eq(teacherId), eq(GroupMemberRole.TEACHER));
    }

    @Test
    void teacherCannotCreateGroupForUnassignedCourse() {
        UUID teacherId = UUID.randomUUID();
        UUID assignedTeacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(authenticatedUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TEACHER"));
        when(courseServiceClient.lookupCourseById(courseId, AUTH_HEADER)).thenReturn(successCourse(courseId, assignedTeacherId));

        assertThrows(
                TeacherNotAssignedToCourseException.class,
                () -> communicationGroupService.createGroup(validRequest(courseId), AUTH_HEADER)
        );
        verify(communicationGroupRepository, never()).save(any());
    }

    @Test
    void studentCannotCreateGroup() {
        when(authenticatedUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_STUDENT"));

        assertThrows(
                UnauthorizedGroupAccessException.class,
                () -> communicationGroupService.createGroup(validRequest(UUID.randomUUID()), AUTH_HEADER)
        );
        verify(courseServiceClient, never()).lookupCourseById(any(), any());
    }

    @Test
    void invalidCourseIsRejected() {
        UUID adminId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(adminId);
        when(authenticatedUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_ADMIN"));
        when(courseServiceClient.lookupCourseById(courseId, AUTH_HEADER))
                .thenReturn(ApiResponse.<CourseLookupResponse>builder().status(HttpStatus.OK).data(null).build());

        assertThrows(
                CourseNotFoundException.class,
                () -> communicationGroupService.createGroup(validRequest(courseId), AUTH_HEADER)
        );
    }

    @Test
    void duplicateCourseGroupIsRejected() {
        UUID adminId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(adminId);
        when(authenticatedUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_ADMIN"));
        when(courseServiceClient.lookupCourseById(courseId, AUTH_HEADER)).thenReturn(successCourse(courseId, UUID.randomUUID()));
        when(communicationGroupRepository.existsByCourseId(courseId)).thenReturn(true);

        assertThrows(
                DuplicateCommunicationGroupException.class,
                () -> communicationGroupService.createGroup(validRequest(courseId), AUTH_HEADER)
        );
        verify(communicationGroupRepository, never()).save(any());
    }

    @Test
    void currentUserReceivesOnlyActiveGroupsTheyBelongTo() {
        UUID userId = UUID.randomUUID();
        CommunicationGroup activeGroup = group(UUID.randomUUID(), GroupStatus.ACTIVE);
        CommunicationGroup archivedGroup = group(UUID.randomUUID(), GroupStatus.ARCHIVED);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(groupMemberRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(List.of(
                member(activeGroup, userId, GroupMemberRole.STUDENT),
                member(archivedGroup, userId, GroupMemberRole.STUDENT)
        ));

        List<CommunicationGroupResponse> response = communicationGroupService.getCurrentUserGroups();

        assertEquals(1, response.size());
        assertEquals(activeGroup.getId(), response.get(0).getId());
    }

    @Test
    void memberCanRetrieveGroup() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(communicationGroupRepository.findByIdAndStatus(groupId, GroupStatus.ACTIVE)).thenReturn(Optional.of(group));

        CommunicationGroupResponse response = communicationGroupService.getGroupById(groupId);

        assertEquals(groupId, response.getId());
        verify(groupMemberService).validateActiveMembership(groupId, userId);
    }

    @Test
    void authorizedTeacherCanArchiveGroup() {
        UUID groupId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);
        GroupMember member = member(group, teacherId, GroupMemberRole.TEACHER);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(authenticatedUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TEACHER"));
        when(communicationGroupRepository.findByIdAndStatus(groupId, GroupStatus.ACTIVE)).thenReturn(Optional.of(group));
        when(groupMemberService.validateActiveMembership(groupId, teacherId)).thenReturn(member);
        when(communicationGroupRepository.save(group)).thenReturn(group);

        CommunicationGroupResponse response = communicationGroupService.archiveGroup(groupId);

        assertEquals(GroupStatus.ARCHIVED, response.getStatus());
    }

    @Test
    void unauthorizedMemberCannotArchiveGroup() {
        UUID groupId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);
        GroupMember member = member(group, studentId, GroupMemberRole.STUDENT);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(studentId);
        when(authenticatedUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TEACHER"));
        when(communicationGroupRepository.findByIdAndStatus(groupId, GroupStatus.ACTIVE)).thenReturn(Optional.of(group));
        when(groupMemberService.validateActiveMembership(groupId, studentId)).thenReturn(member);

        assertThrows(UnauthorizedGroupAccessException.class, () -> communicationGroupService.archiveGroup(groupId));
    }

    private CreateCommunicationGroupRequest validRequest(UUID courseId) {
        return CreateCommunicationGroupRequest.builder()
                .courseId(courseId)
                .name("Java Programming Discussion")
                .description("Official communication group")
                .build();
    }

    private ApiResponse<CourseLookupResponse> successCourse(UUID courseId, UUID teacherId) {
        CourseLookupResponse course = new CourseLookupResponse();
        course.setId(courseId);
        course.setCourseName("Java Programming");
        course.setAssignedTeacherId(teacherId);

        return ApiResponse.<CourseLookupResponse>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .data(course)
                .build();
    }

    private ApiResponse<List<UUID>> successStudents(List<UUID> studentIds) {
        return ApiResponse.<List<UUID>>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .data(studentIds)
                .build();
    }

    private CommunicationGroup group(UUID groupId, GroupStatus status) {
        CommunicationGroup group = CommunicationGroup.builder()
                .courseId(UUID.randomUUID())
                .name("Java Programming Discussion")
                .description("Official communication group")
                .status(status)
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
