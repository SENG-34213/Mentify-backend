package com.mentify.communication.exception;

import java.util.UUID;

public class GroupArchivedException extends RuntimeException {

    public GroupArchivedException(UUID groupId) {
        super("Communication group is archived and cannot receive new messages: " + groupId);
    }
}
