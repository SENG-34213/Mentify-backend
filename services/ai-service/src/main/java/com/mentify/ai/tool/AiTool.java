package com.mentify.ai.tool;

public interface AiTool<T> {

    String getName();

    T execute(String authorizationHeader);
}
