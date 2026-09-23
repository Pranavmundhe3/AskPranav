package com.askpranav.ai.orchestration;

import com.askpranav.ai.SystemPromptProvider;
import com.askpranav.ai.tools.BiographyTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Tool-calling-only stage, used for the TOOL_CALL route: questions best answered by an exact,
 * structured lookup (e.g. "what certifications does he have") without paying for a vector search
 * that a narrative-focused retrieval step would otherwise perform.
 */
@Component
public class ToolCallAgent {

    private final ChatClient chatClient;
    private final BiographyTools biographyTools;
    private final String systemPrompt;

    public ToolCallAgent(ChatClient chatClient, BiographyTools biographyTools, SystemPromptProvider systemPromptProvider) {
        this.chatClient = chatClient;
        this.biographyTools = biographyTools;
        this.systemPrompt = systemPromptProvider.get();
    }

    public String call(String question) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .tools(biographyTools)
                .call()
                .content();
    }
}
