package com.mentify.communication.exception;

import java.util.UUID;

public class MessageNotFoundException extends RuntimeException {

    public MessageNotFoundException(UUID messageId) {
        super("Message not found with id: '" + messageId + "'");
    }
}
