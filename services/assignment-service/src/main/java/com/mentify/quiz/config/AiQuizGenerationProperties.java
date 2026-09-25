package com.mentify.quiz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Data
@Configuration
@ConfigurationProperties(prefix = "quiz.ai-generation")
public class AiQuizGenerationProperties {

    private int maxQuestionCount = 20;
    private BigDecimal defaultQuestionMarks = BigDecimal.ONE;
}
