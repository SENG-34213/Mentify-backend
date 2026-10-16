package com.mentify.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedOptionResponse {

    private String optionText;
    private Boolean correct;
    private Integer optionOrder;
}
