package com.mentify.exam.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchMarksEntryRequest {

    @NotEmpty(message = "results must contain at least one entry")
    private List<@Valid @NotNull(message = "result entries must not be null") MarkEntryRequest> results;
}