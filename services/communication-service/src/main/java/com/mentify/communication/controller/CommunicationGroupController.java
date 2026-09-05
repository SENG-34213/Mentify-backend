package com.mentify.communication.controller;

import com.mentify.communication.dto.request.CreateCommunicationGroupRequest;
import com.mentify.communication.dto.response.CommunicationGroupResponse;
import com.mentify.communication.dto.response.GroupMemberResponse;
import com.mentify.communication.security.AuthenticatedUserService;
import com.mentify.communication.service.CommunicationGroupService;
import com.mentify.communication.service.GroupMemberService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/communication/groups")
@RequiredArgsConstructor
public class CommunicationGroupController {

    private final CommunicationGroupService communicationGroupService;
    private final GroupMemberService groupMemberService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CommunicationGroupResponse>> createGroup(
            @Valid @RequestBody CreateCommunicationGroupRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CommunicationGroupResponse response = communicationGroupService.createGroup(request, authorizationHeader);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Communication group created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommunicationGroupResponse>>> getCurrentUserGroups() {
        List<CommunicationGroupResponse> response = communicationGroupService.getCurrentUserGroups();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Communication groups fetched successfully", response));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<CommunicationGroupResponse>> getGroupById(@PathVariable UUID groupId) {
        CommunicationGroupResponse response = communicationGroupService.getGroupById(groupId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Communication group fetched successfully", response));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> getGroupMembers(@PathVariable UUID groupId) {
        List<GroupMemberResponse> response = groupMemberService.getActiveGroupMembers(
                groupId,
                authenticatedUserService.getCurrentUserId()
        );

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Group members fetched successfully", response));
    }

    @PostMapping("/{groupId}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CommunicationGroupResponse>> archiveGroup(@PathVariable UUID groupId) {
        CommunicationGroupResponse response = communicationGroupService.archiveGroup(groupId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Communication group archived successfully", response));
    }
}
