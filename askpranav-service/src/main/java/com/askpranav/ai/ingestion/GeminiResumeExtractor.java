package com.askpranav.ai.ingestion;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Structured-output extraction: the model is asked for {@link ResumeData} directly and Spring AI parses its
 * reply into the record. It uses its own instructions rather than the chat guardrail prompt, because this
 * is a transformation task, not a conversation.
 */
@Component
public class GeminiResumeExtractor implements ResumeExtractor {

    private final ChatClient chatClient;
    private final String instructions;

    public GeminiResumeExtractor(ChatClient chatClient,
                                 @Value("classpath:/prompts/resume-extraction-prompt.st") Resource promptResource) {
        this.chatClient = chatClient;
        try {
            this.instructions = new String(promptResource.getContentAsByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load " + promptResource, e);
        }
    }

    @Override
    public ResumeData extract(String resumeText) {
        return chatClient.prompt()
                .system(instructions)
                .user("Resume text:\n\n" + resumeText)
                .call()
                .entity(ResumeData.class);
    }
}
