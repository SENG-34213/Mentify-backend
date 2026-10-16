package com.mentify.ai.prompt;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiPromptRegistry {
    public static final String LESSON_SUMMARY = "LESSON_SUMMARY";
    public static final String QUIZ_GENERATION = "QUIZ_GENERATION";
    public static final String TUTOR_CHAT = "TUTOR_CHAT";
    public static final String FEEDBACK = "FEEDBACK";
    public static final String RUBRIC_ANALYSIS = "RUBRIC_ANALYSIS";
    public static final String V1 = "v1";

    private final Map<String, AiPromptTemplate> templates = new ConcurrentHashMap<>();

    public AiPromptRegistry() {
        register(new AiPromptTemplate(
                LESSON_SUMMARY,
                V1,
                "You summarize lesson content for a learning management system. " +
                        "Provide a concise, structured summary focused on main concepts, key points, and learning objectives.",
                "Title: %s\n\nContent:\n%s"
        ));
    }

    public AiPromptTemplate get(String key, String version) {
        AiPromptTemplate template = templates.get(promptId(key, version));
        if (template == null) {
            throw new IllegalArgumentException("AI prompt template not found: " + promptId(key, version));
        }
        return template;
    }

    private void register(AiPromptTemplate template) {
        templates.put(template.promptId(), template);
    }

    private String promptId(String key, String version) {
        return key + ":" + version;
    }
}
