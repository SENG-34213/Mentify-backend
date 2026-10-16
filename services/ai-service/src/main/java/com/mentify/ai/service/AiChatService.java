package com.mentify.ai.service;

import com.mentify.ai.dto.request.AiChatRequest;
import com.mentify.ai.dto.response.AiChatResponse;

public interface AiChatService {

    AiChatResponse chat(AiChatRequest request, String authorizationHeader);
}
