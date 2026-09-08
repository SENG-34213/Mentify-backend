package com.mentify.communication.service.impl;

import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.dto.response.PageResponse;
import com.mentify.communication.entity.CommunicationGroup;
import com.mentify.communication.entity.Message;
import com.mentify.communication.enums.GroupStatus;
import com.mentify.communication.enums.MessageStatus;
import com.mentify.communication.enums.MessageType;
import com.mentify.communication.exception.CommunicationGroupNotFoundException;
import com.mentify.communication.exception.GroupArchivedException;
import com.mentify.communication.exception.InvalidMessageException;
import com.mentify.communication.mapper.MessageMapper;
import com.mentify.communication.repository.CommunicationGroupRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    public static final int DEFAULT_PAGE_SIZE = 30;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_MESSAGE_LENGTH = 5000;

    private final MessageRepository messageRepository;
    private final CommunicationGroupRepository communicationGroupRepository;
    private final GroupMemberService groupMemberService;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public MessageResponse sendMessage(UUID groupId, SendMessageRequest request) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        CommunicationGroup group = getGroup(groupId);

        if (!GroupStatus.ACTIVE.equals(group.getStatus())) {
            throw new GroupArchivedException(groupId);
        }

        groupMemberService.validateActiveMembership(groupId, userId);

        String content = normalizeContent(request);
        Message message = Message.builder()
                .group(group)
                .senderId(userId)
                .content(content)
                .type(MessageType.TEXT)
                .status(MessageStatus.ACTIVE)
                .sentAt(LocalDateTime.now())
                .build();
        message.setActive(true);
        message.setCreatedBy(userId);
        message.setUpdatedBy(userId);

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
        Page<Message> messages = messageRepository.findByGroup_IdAndStatus(groupId, MessageStatus.ACTIVE, pageable);

        return PageResponse.<MessageResponse>builder()
                .content(messages.map(MessageMapper::toResponse).getContent())
                .page(messages.getNumber())
                .size(messages.getSize())
                .totalElements(messages.getTotalElements())
                .totalPages(messages.getTotalPages())
                .build();
    }

    private CommunicationGroup getGroup(UUID groupId) {
        return communicationGroupRepository.findById(groupId)
                .orElseThrow(() -> new CommunicationGroupNotFoundException(groupId));
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
