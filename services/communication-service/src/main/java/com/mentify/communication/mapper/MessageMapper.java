package com.mentify.communication.mapper;

import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.entity.Message;
import com.mentify.communication.enums.MessageStatus;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .groupId(message.getGroup().getId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .type(message.getType())
                .sentAt(message.getSentAt())
                .editedAt(message.getEditedAt())
                .edited(message.getEditedAt() != null)
                .deletedForEveryone(MessageStatus.DELETED.equals(message.getStatus()))
                .build();
    }
}
