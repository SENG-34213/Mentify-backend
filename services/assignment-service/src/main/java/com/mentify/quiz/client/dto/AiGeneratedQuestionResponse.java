package com.mentify.quiz.client.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiGeneratedQuestionResponse {

    private String questionText;
    private String questionType;
    private Integer questionOrder;
    private List<AiGeneratedOptionResponse> options;
}
