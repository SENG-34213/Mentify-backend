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
import com.mentify.communication.exception.MessageNotFoundException;
import com.mentify.communication.exception.UnauthorizedMessageActionException;
import com.mentify.communication.mapper.MessageMapper;
import com.mentify.communication.repository.CommunicationGroupRepository;
import com.mentify.communication.repository.MessageHiddenForUserRepository;
import com.mentify.communication.repository.MessageRepository;
import com.mentify.communication.security.AuthenticatedUserService;
import com.mentify.communication.service.GroupMemberService;
import com.mentify.communication.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    public static final int DEFAULT_PAGE_SIZE = 30;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_MESSAGE_LENGTH = 5000;

    private final MessageRepository messageRepository;
    private final MessageHiddenForUserRepository messageHiddenForUserRepository;
    private final CommunicationGroupRepository communicationGroupRepository;
    private final GroupMemberService groupMemberService;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public MessageResponse sendMessage(UUID groupId, SendMessageRequest request) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        return sendMessage(groupId, request, userId);
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(UUID groupId, SendMessageRequest request, UUID senderId) {
        CommunicationGroup group = getGroup(groupId);

        if (!GroupStatus.ACTIVE.equals(group.getStatus())) {
            throw new GroupArchivedException(groupId);
        }

        groupMemberService.validateActiveMembership(groupId, senderId);

        String content = normalizeContent(request);
        Message message = Message.builder()
                .group(group)
                .senderId(senderId)
                .content(content)
                .type(MessageType.TEXT)
                .status(MessageStatus.ACTIVE)
                .sentAt(LocalDateTime.now())
                .build();
        message.setActive(true);
        message.setCreatedBy(senderId);
        message.setUpdatedBy(senderId);

        return MessageMapper.toResponse(messageRepository.save(message));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getMessageHistory(UUID groupId, int page, int size) {
        UUID userId = authenticatedUserService.getCurrentUserId();

        getGroup(groupId);
        groupMemberService.validateActiveMembership(groupId, userId);

        Pageable pageable = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Order.desc("sentAt"), Sort.Order.desc("id"))
        );
        Page<Message> messages = messageRepository.findVisibleMessagesByGroupAndStatuses(
            groupId,
            EnumSet.of(MessageStatus.ACTIVE, MessageStatus.DELETED),
            userId,
            pageable
        );

        return PageResponse.<MessageResponse>builder()
                .content(messages.map(MessageMapper::toResponse).getContent())
                .page(messages.getNumber())
                .size(messages.getSize())
                .totalElements(messages.getTotalElements())
                .totalPages(messages.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public MessageResponse updateMessage(UUID groupId, UUID messageId, UpdateMessageRequest request) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Message message = getMessageAndValidateMember(groupId, messageId, userId);

        if (!message.getSenderId().equals(userId)) {
            throw new UnauthorizedMessageActionException("Only the sender can edit this message");
        }

        if (MessageStatus.DELETED.equals(message.getStatus())) {
            throw new InvalidMessageException("Deleted message cannot be edited");
        }

        message.setContent(normalizeContent(request == null ? null : SendMessageRequest.builder()
                .content(request.getContent())
                .build()));
        message.setEditedAt(LocalDateTime.now());
        message.setUpdatedBy(userId);

        return MessageMapper.toResponse(messageRepository.save(message));
    }

    @Override
    @Transactional
    public void deleteMessage(UUID groupId, UUID messageId, MessageDeleteScope scope) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        Message message = getMessageAndValidateMember(groupId, messageId, userId);

        MessageDeleteScope effectiveScope = scope == null ? MessageDeleteScope.ME : scope;
        if (MessageDeleteScope.ME.equals(effectiveScope)) {
            hideMessageForUser(message, userId);
            return;
        }

        if (!canDeleteForEveryone(userId, message)) {
            throw new UnauthorizedMessageActionException("You are not allowed to delete this message for everyone");
        }

        if (!MessageStatus.DELETED.equals(message.getStatus())) {
            message.setStatus(MessageStatus.DELETED);
            message.setContent("This message was deleted");
            message.setDeletedAt(LocalDateTime.now());
            message.setDeletedBy(userId);
            message.setEditedAt(null);
            message.setUpdatedBy(userId);
            messageRepository.save(message);
        }
    }

    private CommunicationGroup getGroup(UUID groupId) {
        return communicationGroupRepository.findById(groupId)
                .orElseThrow(() -> new CommunicationGroupNotFoundException(groupId));
    }

    private Message getMessageAndValidateMember(UUID groupId, UUID messageId, UUID userId) {
        getGroup(groupId);
        groupMemberService.validateActiveMembership(groupId, userId);

        return messageRepository.findByIdAndGroup_Id(messageId, groupId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
    }

    private void hideMessageForUser(Message message, UUID userId) {
        MessageHiddenForUser hidden = messageHiddenForUserRepository.findByMessage_IdAndUserId(message.getId(), userId)
                .orElseGet(() -> MessageHiddenForUser.builder()
                        .message(message)
                        .userId(userId)
                        .hiddenAt(LocalDateTime.now())
                        .build());
        hidden.setActive(true);
        hidden.setHiddenAt(LocalDateTime.now());
        hidden.setUpdatedBy(userId);
        if (hidden.getCreatedBy() == null) {
            hidden.setCreatedBy(userId);
        }
        messageHiddenForUserRepository.save(hidden);
    }

    private boolean canDeleteForEveryone(UUID userId, Message message) {
        if (message.getSenderId().equals(userId)) {
            return true;
        }

        Set<String> roles = authenticatedUserService.getCurrentUserRoles();
        if (roles.contains("ROLE_SUPER_ADMIN") || roles.contains("ROLE_ADMIN") || roles.contains("ROLE_TEACHER")) {
            return true;
        }

        GroupMemberRole memberRole = groupMemberService.validateActiveMembership(message.getGroup().getId(), userId).getRole();
        return GroupMemberRole.ADMIN.equals(memberRole) || GroupMemberRole.TEACHER.equals(memberRole);
    }

    private String normalizeContent(SendMessageRequest request) {
        if (request == null || request.getContent() == null) {
            throw new InvalidMessageException("Message content is required");
        }

        String content = request.getContent().trim();
        if (content.isBlank()) {
            throw new InvalidMessageException("Message content is required");
        }

        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidMessageException("Message content must not exceed " + MAX_MESSAGE_LENGTH + " characters");
        }

        return content;
    }

    private int normalizePage(int page) {
        if (page < 0) {
            throw new InvalidMessageException("Page index must not be negative");
        }

        return page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}
