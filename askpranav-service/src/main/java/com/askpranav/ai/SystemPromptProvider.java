package com.askpranav.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads the guardrailed system prompt once and hands the same text to every stage that needs it
 * (RagQuestionService, ToolCallAgent, WriterAgent) so the guardrails live in exactly one file:
 * prompts/system-prompt.st.
 */
@Component
public class SystemPromptProvider {

    private final String systemPrompt;

    public SystemPromptProvider(@Value("classpath:/prompts/system-prompt.st") Resource systemPromptResource) {
        try {
            this.systemPrompt = new String(systemPromptResource.getContentAsByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load system prompt from " + systemPromptResource, e);
        }
    }

    public String get() {
        return systemPrompt;
    }
}
