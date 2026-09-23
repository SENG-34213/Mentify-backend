package com.mentify.ai.prompt;

public record AiPromptTemplate(
        String key,
        String version,
        String systemPrompt,
        String userInputTemplate
) {
    public String promptId() {
        return key + ":" + version;
    }

    public String renderUserInput(Object... args) {
        return String.format(userInputTemplate, args);
    }
}
