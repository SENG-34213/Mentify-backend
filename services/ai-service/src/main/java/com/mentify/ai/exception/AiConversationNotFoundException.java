package com.mentify.ai.exception;

public class AiConversationNotFoundException extends RuntimeException {

    public AiConversationNotFoundException() {
        super("AI conversation not found");
    }
}
