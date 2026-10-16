package com.mentify.communication.exception;

import java.util.UUID;

public class DuplicateCommunicationGroupException extends RuntimeException {

    public DuplicateCommunicationGroupException(UUID courseId) {
        super("Communication group already exists for course: '" + courseId + "'");
    }
}
