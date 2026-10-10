package com.mentify.communication.service.impl;

import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.request.UpdateMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.dto.response.PageResponse;
import com.mentify.communication.entity.CommunicationGroup;
import com.mentify.communication.entity.Message;
import com.mentify.communication.entity.MessageHiddenForUser;
import com.mentify.communication.enums.GroupMemberRole;
import com.mentify.communication.enums.GroupStatus;
import com.mentify.communication.enums.MessageDeleteScope;
import com.mentify.communication.enums.MessageStatus;
import com.mentify.communication.enums.MessageType;
import com.mentify.communication.exception.CommunicationGroupNotFoundException;
import com.mentify.communication.exception.GroupArchivedException;
import com.mentify.communication.exception.InvalidMessageException;
import com.mentify.communication.exception.UnauthorizedMessageActionException;
import com.mentify.communication.exception.UnauthorizedGroupAccessException;
import com.mentify.communication.repository.CommunicationGroupRepository;
import com.mentify.communication.repository.MessageHiddenForUserRepository;
import com.mentify.communication.repository.MessageRepository;
import com.mentify.communication.security.AuthenticatedUserService;
import com.mentify.communication.service.GroupMemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private CommunicationGroupRepository communicationGroupRepository;

    @Mock
    private MessageHiddenForUserRepository messageHiddenForUserRepository;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void activeGroupMemberCanSendTextMessage() {
        UUID groupId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(senderId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            return message;
        });

        MessageResponse response = messageService.sendMessage(groupId, request("  Hello everyone  "));

        assertEquals(groupId, response.getGroupId());
        assertEquals(senderId, response.getSenderId());
        assertEquals("Hello everyone", response.getContent());
        assertEquals(MessageType.TEXT, response.getType());
        assertNotNull(response.getSentAt());
        verify(groupMemberService).validateActiveMembership(groupId, senderId);
    }

    @Test
    void teacherMemberCanSendTextMessage() {
        UUID groupId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        messageService.sendMessage(groupId, request("Teacher update"));

        verify(groupMemberService).validateActiveMembership(groupId, teacherId);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void studentMemberCanSendTextMessage() {
        UUID groupId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(studentId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        messageService.sendMessage(groupId, request("Student question"));

        verify(groupMemberService).validateActiveMembership(groupId, studentId);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void nonMemberCannotSendMessage() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group(groupId, GroupStatus.ACTIVE)));
        when(groupMemberService.validateActiveMembership(groupId, userId))
                .thenThrow(new UnauthorizedGroupAccessException("You are not an active member of this communication group"));

        assertThrows(
                UnauthorizedGroupAccessException.class,
                () -> messageService.sendMessage(groupId, request("Hello"))
        );
        verify(messageRepository, never()).save(any());
    }

    @Test
    void archivedGroupRejectsNewMessages() {
        UUID groupId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(senderId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group(groupId, GroupStatus.ARCHIVED)));

        assertThrows(
                GroupArchivedException.class,
                () -> messageService.sendMessage(groupId, request("Hello"))
        );
        verify(groupMemberService, never()).validateActiveMembership(any(), any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void invalidGroupReturnsNotFoundOnSend() {
        UUID groupId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThrows(
                CommunicationGroupNotFoundException.class,
                () -> messageService.sendMessage(groupId, request("Hello"))
        );
        verify(messageRepository, never()).save(any());
    }

    @Test
    void blankMessageIsRejected() {
        assertInvalidContent("");
    }

    @Test
    void whitespaceOnlyMessageIsRejected() {
        assertInvalidContent("     ");
    }

    @Test
    void oversizedMessageIsRejected() {
        UUID groupId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(senderId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group(groupId, GroupStatus.ACTIVE)));

        assertThrows(
                InvalidMessageException.class,
                () -> messageService.sendMessage(groupId, request("a".repeat(5001)))
        );
        verify(messageRepository, never()).save(any());
    }

    @Test
    void senderAndMessageMetadataComeFromBackend() {
        UUID groupId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(senderId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(messageRepository.save(messageCaptor.capture())).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            return message;
        });

        messageService.sendMessage(groupId, request("Hello"));

        Message saved = messageCaptor.getValue();
        assertEquals(senderId, saved.getSenderId());
        assertEquals(MessageType.TEXT, saved.getType());
        assertEquals(MessageStatus.ACTIVE, saved.getStatus());
        assertNotNull(saved.getSentAt());
        assertEquals(senderId, saved.getCreatedBy());
        assertEquals(senderId, saved.getUpdatedBy());
        assertTrue(saved.isActive());
    }

    @Test
    void groupMemberCanRetrieveHistory() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Message message = message(group(groupId, GroupStatus.ACTIVE), userId, "Latest", LocalDateTime.now());

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group(groupId, GroupStatus.ACTIVE)));
        when(messageRepository.findVisibleMessagesByGroupAndStatuses(eq(groupId), any(), eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(message)));

        PageResponse<MessageResponse> response = messageService.getMessageHistory(groupId, 0, 30);

        assertEquals(1, response.getContent().size());
        assertEquals(groupId, response.getContent().get(0).getGroupId());
        verify(groupMemberService).validateActiveMembership(groupId, userId);
    }

    @Test
    void nonMemberCannotRetrieveHistory() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group(groupId, GroupStatus.ACTIVE)));
        when(groupMemberService.validateActiveMembership(groupId, userId))
                .thenThrow(new UnauthorizedGroupAccessException("You are not an active member of this communication group"));

        assertThrows(
                UnauthorizedGroupAccessException.class,
                () -> messageService.getMessageHistory(groupId, 0, 30)
        );
        verify(messageRepository, never()).findVisibleMessagesByGroupAndStatuses(any(), any(), any(), any());
    }

    @Test
    void invalidGroupReturnsNotFoundOnHistory() {
        UUID groupId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThrows(
                CommunicationGroupNotFoundException.class,
                () -> messageService.getMessageHistory(groupId, 0, 30)
        );
        verify(messageRepository, never()).findVisibleMessagesByGroupAndStatuses(any(), any(), any(), any());
    }

    @Test
    void historyUsesNewestFirstStableSortingAndPagination() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group(groupId, GroupStatus.ACTIVE)));
        when(messageRepository.findVisibleMessagesByGroupAndStatuses(eq(groupId), any(), eq(userId), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of()));

        messageService.getMessageHistory(groupId, 2, 500);

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(MessageServiceImpl.MAX_PAGE_SIZE, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("sentAt").getDirection());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void negativePageIsRejected() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group(groupId, GroupStatus.ACTIVE)));

        assertThrows(
                InvalidMessageException.class,
                () -> messageService.getMessageHistory(groupId, -1, 30)
        );
    }

    @Test
    void archivedGroupHistoryCanStillBeRead() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group(groupId, GroupStatus.ARCHIVED)));
        when(messageRepository.findVisibleMessagesByGroupAndStatuses(eq(groupId), any(), eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<MessageResponse> response = messageService.getMessageHistory(groupId, 0, 30);

        assertEquals(0, response.getContent().size());
        verify(groupMemberService).validateActiveMembership(groupId, userId);
    }

    @Test
    void senderCanUpdateMessage() {
        UUID groupId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);
        Message message = message(group, senderId, "Old", LocalDateTime.now());
        message.setId(messageId);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(senderId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(messageRepository.findByIdAndGroup_Id(messageId, groupId)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = messageService.updateMessage(
                groupId,
                messageId,
                UpdateMessageRequest.builder().content(" Updated content ").build()
        );

        assertEquals("Updated content", response.getContent());
        assertNotNull(response.getEditedAt());
    }

    @Test
    void nonSenderCannotUpdateMessage() {
        UUID groupId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);
        Message message = message(group, senderId, "Old", LocalDateTime.now());
        message.setId(messageId);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(currentUserId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(messageRepository.findByIdAndGroup_Id(messageId, groupId)).thenReturn(Optional.of(message));

        assertThrows(
                UnauthorizedMessageActionException.class,
                () -> messageService.updateMessage(groupId, messageId, UpdateMessageRequest.builder().content("Edit").build())
        );
    }

    @Test
    void memberCanDeleteForMe() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);
        Message message = message(group, UUID.randomUUID(), "Hello", LocalDateTime.now());
        message.setId(messageId);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(messageRepository.findByIdAndGroup_Id(messageId, groupId)).thenReturn(Optional.of(message));
        when(messageHiddenForUserRepository.findByMessage_IdAndUserId(messageId, userId)).thenReturn(Optional.empty());

        messageService.deleteMessage(groupId, messageId, MessageDeleteScope.ME);

        verify(messageHiddenForUserRepository).save(any(MessageHiddenForUser.class));
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void senderCanDeleteForEveryone() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);
        Message message = message(group, userId, "Hello", LocalDateTime.now());
        message.setId(messageId);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(messageRepository.findByIdAndGroup_Id(messageId, groupId)).thenReturn(Optional.of(message));

        messageService.deleteMessage(groupId, messageId, MessageDeleteScope.EVERYONE);

        assertEquals(MessageStatus.DELETED, message.getStatus());
        assertEquals("This message was deleted", message.getContent());
        verify(messageRepository).save(message);
    }

    @Test
    void teacherRoleCanDeleteOthersMessageForEveryone() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);
        Message message = message(group, UUID.randomUUID(), "Hello", LocalDateTime.now());
        message.setId(messageId);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(authenticatedUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TEACHER"));
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(messageRepository.findByIdAndGroup_Id(messageId, groupId)).thenReturn(Optional.of(message));

        messageService.deleteMessage(groupId, messageId, MessageDeleteScope.EVERYONE);

        verify(messageRepository).save(message);
    }

    @Test
    void studentCannotDeleteOthersMessageForEveryone() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        CommunicationGroup group = group(groupId, GroupStatus.ACTIVE);
        Message message = message(group, UUID.randomUUID(), "Hello", LocalDateTime.now());
        message.setId(messageId);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(authenticatedUserService.getCurrentUserRoles()).thenReturn(Set.of("ROLE_STUDENT"));
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(messageRepository.findByIdAndGroup_Id(messageId, groupId)).thenReturn(Optional.of(message));
        when(groupMemberService.validateActiveMembership(groupId, userId)).thenReturn(member(group, userId, GroupMemberRole.STUDENT));

        assertThrows(
                UnauthorizedMessageActionException.class,
                () -> messageService.deleteMessage(groupId, messageId, MessageDeleteScope.EVERYONE)
        );
    }

    private void assertInvalidContent(String content) {
        UUID groupId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        when(authenticatedUserService.getCurrentUserId()).thenReturn(senderId);
        when(communicationGroupRepository.findById(groupId)).thenReturn(Optional.of(group(groupId, GroupStatus.ACTIVE)));

        assertThrows(
                InvalidMessageException.class,
                () -> messageService.sendMessage(groupId, request(content))
        );
        verify(messageRepository, never()).save(any());
    }

    private SendMessageRequest request(String content) {
        return SendMessageRequest.builder()
                .content(content)
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

    private Message message(CommunicationGroup group, UUID senderId, String content, LocalDateTime sentAt) {
        Message message = Message.builder()
                .group(group)
                .senderId(senderId)
                .content(content)
                .type(MessageType.TEXT)
                .status(MessageStatus.ACTIVE)
                .sentAt(sentAt)
                .build();
        message.setId(UUID.randomUUID());
        return message;
    }

    private com.mentify.communication.entity.GroupMember member(CommunicationGroup group, UUID userId, GroupMemberRole role) {
        com.mentify.communication.entity.GroupMember member = com.mentify.communication.entity.GroupMember.builder()
                .group(group)
                .userId(userId)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .build();
        member.setActive(true);
        return member;
    }
}
