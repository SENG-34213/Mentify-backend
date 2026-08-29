package com.mentify.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum MaterialType {
    VIDEO,
    IMAGE,
    AUDIO,
    PDF,
    PPT,
    DOC;

    @JsonCreator
    public static MaterialType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Learning material type is required");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("PPTS".equals(normalized)) {
            return PPT;
        }
        if ("DOCS".equals(normalized)) {
            return DOC;
        }

        return MaterialType.valueOf(normalized);
    }
}
