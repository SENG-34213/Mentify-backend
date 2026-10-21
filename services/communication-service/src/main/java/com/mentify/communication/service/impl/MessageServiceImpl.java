package com.mentify.communication.service.impl;

import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.request.UpdateMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.dto.response.PageResponse;
import com.mentify.communication.dto.response.CommunicationAttachmentResponse;
import com.mentify.communication.dto.response.AttachmentDownloadResponse;
import com.mentify.communication.client.MaterialServiceClient;
import com.mentify.communication.client.dto.MaterialDownloadResponse;
import com.mentify.communication.client.dto.MaterialLookupResponse;
import com.mentify.communication.client.dto.MaterialUploadResponse;
import com.mentify.communication.entity.CommunicationGroup;
import com.mentify.communication.entity.Message;
import com.mentify.communication.entity.MessageAttachment;
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
import com.mentify.communication.repository.MessageAttachmentRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import org.springframework.security.access.AccessDeniedException;

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
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MaterialServiceClient materialServiceClient;

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
        attachMaterials(message, group, request, senderId);
        if (message.getAttachments().isEmpty() && (content == null || content.isBlank())) {
            throw new InvalidMessageException("Message content or at least one attachment is required");
        }
        message.setActive(true);
        message.setCreatedBy(senderId);
        message.setUpdatedBy(senderId);

        return MessageMapper.toResponse(messageRepository.save(message));
    }

    @Override
    @Transactional
    public CommunicationAttachmentResponse uploadAttachment(UUID groupId, MultipartFile file) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        CommunicationGroup group = getGroup(groupId);
        ensureActiveMember(group, userId);
        if (file == null || file.isEmpty()) {
            throw new InvalidMessageException("Attachment file is required");
        }

        String filename = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "communication-attachment" : file.getOriginalFilename();
        var upload = materialServiceClient.upload(
                filename,
                "Communication attachment",
                "COMMUNICATION_ATTACHMENT",
                group.getCourseId(),
                file,
                authenticatedUserService.getAuthorizationHeader()
        );
        if (upload == null || upload.getData() == null || upload.getData().id() == null) {
            throw new IllegalStateException("Material Service did not return the uploaded attachment");
        }
        return toAttachmentResponse(upload.getData());
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDownloadResponse downloadAttachment(UUID groupId, UUID messageId, UUID attachmentId) {
        UUID userId = authenticatedUserService.getCurrentUserId();
        CommunicationGroup group = getGroup(groupId);
        ensureActiveMember(group, userId);
        MessageAttachment attachment = messageAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
        if (!attachment.getMessage().getId().equals(messageId)
                || !attachment.getMessage().getGroup().getId().equals(groupId)) {
            throw new MessageNotFoundException(messageId);
        }
        if (MessageStatus.DELETED.equals(attachment.getMessage().getStatus())) {
            throw new MessageNotFoundException(messageId);
        }
        MaterialDownloadResponse download = requireData(materialServiceClient.createDownload(
                attachment.getMaterialId(), authenticatedUserService.getAuthorizationHeader()));
        return new AttachmentDownloadResponse(
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                download.downloadUrl(),
                download.expiresAt()
        );
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
            return null;
        }

        String content = request.getContent().trim();
        if (content.isBlank()) {
            return null;
        }

        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidMessageException("Message content must not exceed " + MAX_MESSAGE_LENGTH + " characters");
        }

        return content;
    }

    private void attachMaterials(Message message, CommunicationGroup group, SendMessageRequest request, UUID senderId) {
        if (request == null || request.getAttachmentIds() == null || request.getAttachmentIds().isEmpty()) {
            return;
        }
        List<UUID> ids = request.getAttachmentIds();
        if (ids.stream().anyMatch(id -> id == null) || ids.size() != new HashSet<>(ids).size()) {
            throw new InvalidMessageException("Attachment ids must be unique and valid");
        }
        for (UUID materialId : ids) {
            MaterialLookupResponse material = requireData(materialServiceClient.getMaterial(
                    materialId, authenticatedUserService.getAuthorizationHeader()));
            if (!senderId.equals(material.uploadedBy()) || !group.getCourseId().equals(material.courseId())) {
                throw new AccessDeniedException("Attachment does not belong to the sender or group course");
            }
            MessageAttachment attachment = MessageAttachment.builder()
                    .message(message)
                    .materialId(material.id())
                    .originalFilename(material.originalFilename())
                    .contentType(material.contentType())
                    .fileSize(material.fileSize() == null ? 0L : material.fileSize())
                    .checksum(material.checksum())
                    .attachmentType(resolveAttachmentType(material.contentType()))
                    .build();
            message.getAttachments().add(attachment);
        }
        message.setType(contentIsPresent(message.getContent())
                ? MessageType.TEXT_WITH_ATTACHMENT
                : resolveMessageType(message.getAttachments()));
    }

    private void ensureActiveMember(CommunicationGroup group, UUID userId) {
        if (!GroupStatus.ACTIVE.equals(group.getStatus())) {
            throw new GroupArchivedException(group.getId());
        }
        groupMemberService.validateActiveMembership(group.getId(), userId);
    }

    private MessageType resolveMessageType(List<MessageAttachment> attachments) {
        if (attachments.isEmpty()) return MessageType.TEXT;
        if (attachments.size() == 1
                && attachments.get(0).getAttachmentType() == com.mentify.communication.enums.AttachmentType.AUDIO) {
            return MessageType.VOICE;
        }
        return attachments.size() == 1 && attachments.get(0).getAttachmentType() == com.mentify.communication.enums.AttachmentType.IMAGE
                ? MessageType.IMAGE : MessageType.FILE;
    }

    private boolean contentIsPresent(String content) {
        return content != null && !content.isBlank();
    }

    private com.mentify.communication.enums.AttachmentType resolveAttachmentType(String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.startsWith("image/")) return com.mentify.communication.enums.AttachmentType.IMAGE;
        if (type.startsWith("video/")) return com.mentify.communication.enums.AttachmentType.VIDEO;
        if (type.startsWith("audio/")) return com.mentify.communication.enums.AttachmentType.AUDIO;
        if (type.startsWith("text/") || type.contains("pdf") || type.contains("word") || type.contains("excel")
                || type.contains("sheet") || type.contains("presentation") || type.contains("zip")) {
            return com.mentify.communication.enums.AttachmentType.DOCUMENT;
        }
        return com.mentify.communication.enums.AttachmentType.OTHER;
    }

    private CommunicationAttachmentResponse toAttachmentResponse(MaterialUploadResponse material) {
        return CommunicationAttachmentResponse.builder()
                .id(material.id())
                .materialId(material.id())
                .originalFilename(material.originalFilename())
                .contentType(material.contentType())
                .fileSize(material.fileSize())
                .checksum(material.checksum())
                .attachmentType(resolveAttachmentType(material.contentType()))
                .build();
    }

    private <T> T requireData(com.mentify.payload.response.ApiResponse<T> response) {
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Material Service returned no attachment data");
        }
        return response.getData();
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
