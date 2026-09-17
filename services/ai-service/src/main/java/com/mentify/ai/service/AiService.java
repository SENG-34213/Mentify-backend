package com.mentify.ai.service;
 
import com.mentify.ai.dto.request.AiGenerateRequest;
import com.mentify.ai.dto.response.AiGenerateResponse;
 
import java.util.Map;
 
public interface AiService {
    Map<String, String> getServiceStatus();
 
    AiGenerateResponse generate(AiGenerateRequest request);
}
