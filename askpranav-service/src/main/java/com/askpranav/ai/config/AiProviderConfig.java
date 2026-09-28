package com.askpranav.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the single {@link ChatClient} the rest of the app depends on. The only model on the
 * classpath is Gemini (Spring AI's native Google GenAI starter), so the auto-configured
 * {@link ChatModel} is injected directly; the model itself is chosen with
 * {@code ASKPRANAV_GEMINI_MODEL}.
 */
@Configuration
public class AiProviderConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
