package com.mentify.quiz.client.dto;

import lombok.Data;

@Data
public class AiGeneratedOptionResponse {

    private String optionText;
    private Boolean correct;
    private Integer optionOrder;
}
