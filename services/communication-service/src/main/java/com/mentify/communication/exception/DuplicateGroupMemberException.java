package com.mentify.communication.exception;

import java.util.UUID;

public class DuplicateGroupMemberException extends RuntimeException {

    public DuplicateGroupMemberException(UUID groupId, UUID userId) {
        super("Group member already exists for group '" + groupId + "' and user '" + userId + "'");
    }
}
