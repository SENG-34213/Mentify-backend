package com.mentify.communication.service;

import com.mentify.communication.TestJwtDecoderConfig;
import com.mentify.communication.client.CourseServiceClient;
import com.mentify.communication.client.EntrollmentServiceClient;
import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.request.UpdateMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.dto.response.PageResponse;
import com.mentify.communication.entity.CommunicationGroup;
import com.mentify.communication.entity.GroupMember;
import com.mentify.communication.entity.Message;
import com.mentify.communication.enums.GroupMemberRole;
import com.mentify.communication.enums.GroupStatus;
import com.mentify.communication.enums.MessageDeleteScope;
import com.mentify.communication.enums.MessageStatus;
import com.mentify.communication.enums.MessageType;
import com.mentify.communication.exception.CommunicationGroupNotFoundException;
import com.mentify.communication.exception.UnauthorizedGroupAccessException;
import com.mentify.communication.repository.CommunicationGroupRepository;
import com.mentify.communication.repository.GroupMemberRepository;
import com.mentify.communication.repository.MessageRepository;
import com.mentify.communication.security.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:message_service_integration_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "mentify.security.enabled=true"
})
@Import(TestJwtDecoderConfig.class)
@Transactional
class MessageServiceIntegrationTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private CommunicationGroupRepository communicationGroupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @MockBean
    private AuthenticatedUserService authenticatedUserService;

    @MockBean
    private CourseServiceClient courseServiceClient;

    @MockBean
    private EntrollmentServiceClient entrollmentServiceClient;

    @Test
    void messageIsPersistedForActiveMember() {
        UUID senderId = UUID.randomUUID();
        CommunicationGroup group = communicationGroupRepository.save(group(GroupStatus.ACTIVE));
        groupMemberRepository.save(member(group, senderId, GroupMemberRole.STUDENT));
        when(authenticatedUserService.getCurrentUserId()).thenReturn(senderId);

        MessageResponse response = messageService.sendMessage(
                group.getId(),
                SendMessageRequest.builder().content("  Hello everyone  ").build()
        );

        List<Message> savedMessages = messageRepository.findAll();
        assertThat(savedMessages).hasSize(1);
        assertThat(response.getId()).isEqualTo(savedMessages.get(0).getId());
        assertThat(response.getGroupId()).isEqualTo(group.getId());
        assertThat(response.getSenderId()).isEqualTo(senderId);
        assertThat(response.getContent()).isEqualTo("Hello everyone");
        assertThat(savedMessages.get(0).getType()).isEqualTo(MessageType.TEXT);
        assertThat(savedMessages.get(0).getStatus()).isEqualTo(MessageStatus.ACTIVE);
        assertThat(savedMessages.get(0).getSentAt()).isNotNull();
    }

    @Test
    void nonMemberCannotRetrieveHistory() {
        UUID userId = UUID.randomUUID();
        CommunicationGroup group = communicationGroupRepository.save(group(GroupStatus.ACTIVE));
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);

        assertThrows(
                UnauthorizedGroupAccessException.class,
                () -> messageService.getMessageHistory(group.getId(), 0, 30)
        );
    }

    @Test
    void invalidGroupReturnsNotFoundForHistory() {
        when(authenticatedUserService.getCurrentUserId()).thenReturn(UUID.randomUUID());

        assertThrows(
                CommunicationGroupNotFoundException.class,
                () -> messageService.getMessageHistory(UUID.randomUUID(), 0, 30)
        );
    }

    @Test
    void historyReturnsOnlyRequestedGroupNewestFirst() {
        UUID userId = UUID.randomUUID();
        CommunicationGroup requestedGroup = communicationGroupRepository.save(group(GroupStatus.ACTIVE));
        CommunicationGroup otherGroup = communicationGroupRepository.save(group(GroupStatus.ACTIVE));
        groupMemberRepository.save(member(requestedGroup, userId, GroupMemberRole.STUDENT));
        LocalDateTime baseTime = LocalDateTime.of(2026, 9, 6, 10, 0);
        messageRepository.save(message(requestedGroup, userId, "Old", baseTime));
        messageRepository.save(message(otherGroup, userId, "Other group", baseTime.plusMinutes(10)));
        messageRepository.save(message(requestedGroup, userId, "Latest", baseTime.plusMinutes(20)));
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);

        PageResponse<MessageResponse> response = messageService.getMessageHistory(requestedGroup.getId(), 0, 30);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent()).extracting(MessageResponse::getGroupId)
                .containsOnly(requestedGroup.getId());
        assertThat(response.getContent()).extracting(MessageResponse::getContent)
                .containsExactly("Latest", "Old");
    }

    @Test
    void paginationAndPageSizeLimitWork() {
        UUID userId = UUID.randomUUID();
        CommunicationGroup group = communicationGroupRepository.save(group(GroupStatus.ACTIVE));
        groupMemberRepository.save(member(group, userId, GroupMemberRole.STUDENT));
        LocalDateTime baseTime = LocalDateTime.of(2026, 9, 6, 10, 0);
        for (int i = 0; i < 105; i++) {
            messageRepository.save(message(group, userId, "Message " + i, baseTime.plusSeconds(i)));
        }
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);

        PageResponse<MessageResponse> firstPage = messageService.getMessageHistory(group.getId(), 0, 500);
        PageResponse<MessageResponse> secondPage = messageService.getMessageHistory(group.getId(), 1, 100);

        assertThat(firstPage.getSize()).isEqualTo(100);
        assertThat(firstPage.getContent()).hasSize(100);
        assertThat(firstPage.getTotalElements()).isEqualTo(105);
        assertThat(secondPage.getContent()).hasSize(5);
    }

    @Test
    void archivedGroupHistoryCanStillBeRead() {
        UUID userId = UUID.randomUUID();
        CommunicationGroup group = communicationGroupRepository.save(group(GroupStatus.ARCHIVED));
        groupMemberRepository.save(member(group, userId, GroupMemberRole.STUDENT));
        messageRepository.save(message(group, userId, "Archived history", LocalDateTime.of(2026, 9, 6, 10, 0)));
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);

        PageResponse<MessageResponse> response = messageService.getMessageHistory(group.getId(), 0, 30);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getContent()).isEqualTo("Archived history");
    }

    @Test
    void senderCanUpdateMessage() {
        UUID senderId = UUID.randomUUID();
        CommunicationGroup group = communicationGroupRepository.save(group(GroupStatus.ACTIVE));
        groupMemberRepository.save(member(group, senderId, GroupMemberRole.STUDENT));
        Message message = messageRepository.save(message(group, senderId, "Old message", LocalDateTime.now()));
        when(authenticatedUserService.getCurrentUserId()).thenReturn(senderId);

        MessageResponse response = messageService.updateMessage(
                group.getId(),
                message.getId(),
                UpdateMessageRequest.builder().content("Updated message").build()
        );

        assertThat(response.getContent()).isEqualTo("Updated message");
        assertThat(response.isEdited()).isTrue();
        assertThat(response.getEditedAt()).isNotNull();
    }

    @Test
    void deleteForMeHidesMessageOnlyForThatUser() {
        UUID senderId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        CommunicationGroup group = communicationGroupRepository.save(group(GroupStatus.ACTIVE));
        groupMemberRepository.save(member(group, senderId, GroupMemberRole.STUDENT));
        groupMemberRepository.save(member(group, secondUserId, GroupMemberRole.STUDENT));
        Message message = messageRepository.save(message(group, senderId, "Delete only for me", LocalDateTime.now()));

        when(authenticatedUserService.getCurrentUserId()).thenReturn(senderId);
        messageService.deleteMessage(group.getId(), message.getId(), MessageDeleteScope.ME);
        PageResponse<MessageResponse> senderHistory = messageService.getMessageHistory(group.getId(), 0, 30);

        when(authenticatedUserService.getCurrentUserId()).thenReturn(secondUserId);
        PageResponse<MessageResponse> secondUserHistory = messageService.getMessageHistory(group.getId(), 0, 30);

        assertThat(senderHistory.getContent()).isEmpty();
        assertThat(secondUserHistory.getContent()).hasSize(1);
    }

    @Test
    void teacherCanDeleteStudentMessageForEveryone() {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        CommunicationGroup group = communicationGroupRepository.save(group(GroupStatus.ACTIVE));
        groupMemberRepository.save(member(group, studentId, GroupMemberRole.STUDENT));
        groupMemberRepository.save(member(group, teacherId, GroupMemberRole.TEACHER));
        Message message = messageRepository.save(message(group, studentId, "Delete for everyone", LocalDateTime.now()));
        when(authenticatedUserService.getCurrentUserId()).thenReturn(teacherId);
        when(authenticatedUserService.getCurrentUserRoles()).thenReturn(java.util.Set.of("ROLE_TEACHER"));

        messageService.deleteMessage(group.getId(), message.getId(), MessageDeleteScope.EVERYONE);
        PageResponse<MessageResponse> history = messageService.getMessageHistory(group.getId(), 0, 30);

        assertThat(history.getContent()).hasSize(1);
        assertThat(history.getContent().get(0).isDeletedForEveryone()).isTrue();
        assertThat(history.getContent().get(0).getContent()).isEqualTo("This message was deleted");
    }

    private CommunicationGroup group(GroupStatus status) {
        return CommunicationGroup.builder()
                .courseId(UUID.randomUUID())
                .name("Java Programming Discussion")
                .description("Official communication group")
                .status(status)
                .build();
    }

    private GroupMember member(CommunicationGroup group, UUID userId, GroupMemberRole role) {
        GroupMember member = GroupMember.builder()
                .group(group)
                .userId(userId)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .build();
        member.setActive(true);
        return member;
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
        message.setActive(true);
        return message;
    }
}
