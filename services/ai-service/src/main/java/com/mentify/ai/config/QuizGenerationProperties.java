package com.mentify.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.quiz-generation")
public class QuizGenerationProperties {

    private long maxFileSizeBytes = 10 * 1024 * 1024;
    private int maxQuestionCount = 20;
    private int maxDocumentCharacters = 12000;
    private int minDocumentCharactersPerQuestion = 120;
    private int maxOutputTokens = 2500;
    private double temperature = 0.2;
    private int regenerationAttempts = 1;
}
