package com.mentify.communication.service.impl;

import com.mentify.communication.client.CourseServiceClient;
import com.mentify.communication.client.EntrollmentServiceClient;
import com.mentify.communication.client.dto.CourseLookupResponse;
import com.mentify.communication.dto.request.CreateCommunicationGroupRequest;
import com.mentify.communication.dto.response.CommunicationGroupResponse;
import com.mentify.communication.dto.response.GroupMemberResponse;
import com.mentify.communication.entity.CommunicationGroup;
import com.mentify.communication.entity.GroupMember;
import com.mentify.communication.enums.GroupMemberRole;
import com.mentify.communication.enums.GroupStatus;
import com.mentify.communication.exception.CommunicationGroupNotFoundException;
import com.mentify.communication.exception.CourseNotFoundException;
import com.mentify.communication.exception.DuplicateCommunicationGroupException;
import com.mentify.communication.exception.TeacherNotAssignedToCourseException;
import com.mentify.communication.exception.UnauthorizedGroupAccessException;
import com.mentify.communication.mapper.CommunicationGroupMapper;
import com.mentify.communication.repository.CommunicationGroupRepository;
import com.mentify.communication.repository.GroupMemberRepository;
import com.mentify.communication.security.AuthenticatedUserService;
import com.mentify.communication.service.CommunicationGroupService;
import com.mentify.communication.service.GroupMemberService;
import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunicationGroupServiceImpl implements CommunicationGroupService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_TEACHER = "ROLE_TEACHER";

    private final CommunicationGroupRepository communicationGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMemberService groupMemberService;
    private final CourseServiceClient courseServiceClient;
    private final EntrollmentServiceClient entrollmentServiceClient;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public CommunicationGroupResponse createGroup(CreateCommunicationGroupRequest request, String authorizationHeader) {
        UUID currentUserId = authenticatedUserService.getCurrentUserId();
        Set<String> roles = authenticatedUserService.getCurrentUserRoles();

        validateCanCreateGroup(roles);

        CourseLookupResponse course = lookupCourse(request.getCourseId(), authorizationHeader);

        if (isTeacher(roles) && !currentUserId.equals(course.getAssignedTeacherId())) {
            throw new TeacherNotAssignedToCourseException(currentUserId, request.getCourseId());
        }

        if (communicationGroupRepository.existsByCourseId(request.getCourseId())) {
            throw new DuplicateCommunicationGroupException(request.getCourseId());
        }

        CommunicationGroup group = CommunicationGroup.builder()
                .courseId(request.getCourseId())
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .status(GroupStatus.ACTIVE)
                .build();
        group.setCreatedBy(currentUserId);
        group.setUpdatedBy(currentUserId);

        CommunicationGroup savedGroup = communicationGroupRepository.save(group);

        groupMemberService.addGroupMember(savedGroup, currentUserId, roleForCreator(roles));
        addAssignedTeacher(savedGroup, course.getAssignedTeacherId());
        addEnrolledStudents(savedGroup, request.getCourseId(), authorizationHeader);

        log.info("Communication group [{}] created for course [{}]", savedGroup.getId(), savedGroup.getCourseId());

        return CommunicationGroupMapper.toGroupResponse(savedGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunicationGroupResponse> getCurrentUserGroups() {
        UUID currentUserId = authenticatedUserService.getCurrentUserId();

        return groupMemberRepository.findByUserIdAndIsActiveTrue(currentUserId).stream()
                .map(GroupMember::getGroup)
                .filter(group -> GroupStatus.ACTIVE.equals(group.getStatus()))
                .map(CommunicationGroupMapper::toGroupResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommunicationGroupResponse getGroupById(UUID groupId) {
        UUID currentUserId = authenticatedUserService.getCurrentUserId();
        CommunicationGroup group = findActiveGroup(groupId);

        groupMemberService.validateActiveMembership(group.getId(), currentUserId);

        return CommunicationGroupMapper.toGroupResponse(group);
    }

    @Override
    @Transactional
    public GroupMemberResponse addStudentToGroup(UUID groupId, UUID studentId) {
        CommunicationGroup group = findActiveGroup(groupId);
        validateCanManageGroupMembers(group);

        GroupMember member = addStudentMemberIfAbsent(group, studentId);

        log.info("Student [{}] added to communication group [{}]", studentId, groupId);

        return CommunicationGroupMapper.toMemberResponse(member);
    }

    @Override
    @Transactional
    public GroupMemberResponse addStudentToCourseGroup(UUID courseId, UUID studentId) {
        CommunicationGroup group = communicationGroupRepository.findByCourseIdAndStatus(courseId, GroupStatus.ACTIVE)
                .orElseThrow(() -> new CommunicationGroupNotFoundException("course id", courseId));
        validateCanManageGroupMembers(group);

        GroupMember member = addStudentMemberIfAbsent(group, studentId);

        log.info("Student [{}] added to communication group [{}] for course [{}]", studentId, group.getId(), courseId);

        return CommunicationGroupMapper.toMemberResponse(member);
    }

    @Override
    @Transactional
    public CommunicationGroupResponse archiveGroup(UUID groupId) {
        UUID currentUserId = authenticatedUserService.getCurrentUserId();
        Set<String> roles = authenticatedUserService.getCurrentUserRoles();
        CommunicationGroup group = findActiveGroup(groupId);

        if (!isAdmin(roles)) {
            GroupMember member = groupMemberService.validateActiveMembership(groupId, currentUserId);
            if (!GroupMemberRole.TEACHER.equals(member.getRole())) {
                throw new UnauthorizedGroupAccessException("Only admins and authorized teachers can archive communication groups");
            }
        }

        group.setStatus(GroupStatus.ARCHIVED);
        CommunicationGroup savedGroup = communicationGroupRepository.save(group);

        return CommunicationGroupMapper.toGroupResponse(savedGroup);
    }

    private void validateCanCreateGroup(Set<String> roles) {
        if (!isAdmin(roles) && !isTeacher(roles)) {
            throw new UnauthorizedGroupAccessException("Only admins and teachers can create communication groups");
        }
    }

    private void validateCanManageGroupMembers(CommunicationGroup group) {
        UUID currentUserId = authenticatedUserService.getCurrentUserId();
        Set<String> roles = authenticatedUserService.getCurrentUserRoles();

        if (isAdmin(roles)) {
            return;
        }

        if (isTeacher(roles)) {
            GroupMember member = groupMemberService.validateActiveMembership(group.getId(), currentUserId);
            if (GroupMemberRole.TEACHER.equals(member.getRole())) {
                return;
            }
        }

        throw new UnauthorizedGroupAccessException("Only admins and authorized teachers can add students to communication groups");
    }

    private GroupMember addStudentMemberIfAbsent(CommunicationGroup group, UUID studentId) {
        return groupMemberRepository.findByGroup_IdAndUserIdAndIsActiveTrue(group.getId(), studentId)
                .orElseGet(() -> {
                    List<GroupMember> createdMembers = groupMemberService.addGroupMembers(
                            group,
                            List.of(studentId),
                            GroupMemberRole.STUDENT
                    );

                    if (!createdMembers.isEmpty()) {
                        return createdMembers.get(0);
                    }

                    return groupMemberRepository.findByGroup_IdAndUserIdAndIsActiveTrue(group.getId(), studentId)
                            .orElseThrow(() -> new IllegalStateException("Failed to add student to communication group"));
                });
    }

    private CourseLookupResponse lookupCourse(UUID courseId, String authorizationHeader) {
        try {
            ApiResponse<CourseLookupResponse> response = courseServiceClient.lookupCourseById(courseId, authorizationHeader);
            CourseLookupResponse course = response.getData();

            if (course == null || course.getId() == null) {
                throw new CourseNotFoundException(courseId);
            }

            return course;
        } catch (FeignException.NotFound ex) {
            throw new CourseNotFoundException(courseId);
        } catch (FeignException ex) {
            throw new IllegalStateException("Failed to validate course", ex);
        }
    }

    private void addAssignedTeacher(CommunicationGroup group, UUID teacherId) {
        if (teacherId != null) {
            groupMemberService.addGroupMembers(group, List.of(teacherId), GroupMemberRole.TEACHER);
        }
    }

    private void addEnrolledStudents(CommunicationGroup group, UUID courseId, String authorizationHeader) {
        List<UUID> studentIds = fetchEnrolledStudentIds(courseId, authorizationHeader);
        groupMemberService.addGroupMembers(group, studentIds, GroupMemberRole.STUDENT);
    }

    private List<UUID> fetchEnrolledStudentIds(UUID courseId, String authorizationHeader) {
        try {
            ApiResponse<List<UUID>> response = entrollmentServiceClient.getEnrolledStudentIdsByCourse(courseId, authorizationHeader);
            List<UUID> studentIds = response.getData() != null ? response.getData() : List.of();

            return new LinkedHashSet<>(studentIds).stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (FeignException.NotFound ex) {
            return List.of();
        } catch (FeignException ex) {
            throw new IllegalStateException("Failed to retrieve enrolled students", ex);
        }
    }

    private CommunicationGroup findActiveGroup(UUID groupId) {
        return communicationGroupRepository.findByIdAndStatus(groupId, GroupStatus.ACTIVE)
                .orElseThrow(() -> new CommunicationGroupNotFoundException(groupId));
    }

    private GroupMemberRole roleForCreator(Set<String> roles) {
        if (isAdmin(roles)) {
            return GroupMemberRole.ADMIN;
        }

        return GroupMemberRole.TEACHER;
    }

    private boolean isAdmin(Set<String> roles) {
        return roles.contains(ROLE_ADMIN) || roles.contains(ROLE_SUPER_ADMIN);
    }

    private boolean isTeacher(Set<String> roles) {
        return roles.contains(ROLE_TEACHER);
    }
}
