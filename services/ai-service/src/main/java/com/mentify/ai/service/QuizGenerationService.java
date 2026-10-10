package com.mentify.ai.service;

import com.mentify.ai.dto.request.QuizGenerationRequest;
import com.mentify.ai.dto.response.GeneratedQuizDraftResponse;
import org.springframework.web.multipart.MultipartFile;

public interface QuizGenerationService {

    GeneratedQuizDraftResponse generateQuiz(QuizGenerationRequest request, MultipartFile file);
}
