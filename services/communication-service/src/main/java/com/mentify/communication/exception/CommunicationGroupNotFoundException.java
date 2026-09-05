package com.mentify.communication.exception;

import java.util.UUID;

public class CommunicationGroupNotFoundException extends RuntimeException {

    public CommunicationGroupNotFoundException(UUID groupId) {
        super("Communication group not found with id: '" + groupId + "'");
    }
}
