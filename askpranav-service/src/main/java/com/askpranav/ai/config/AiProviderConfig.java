package com.askpranav.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The model-agnostic layer: {@code askpranav.ai.provider} picks which of the auto-configured
 * ChatModel beans (OpenAI / Anthropic / Gemini - each provided by its own Spring AI starter on the
 * classpath) backs the single {@link ChatClient} the rest of the app depends on. Swapping providers
 * is a config change, not a code change - the point the "model-agnostic" requirement is asking for.
 *
 * VERIFY at build time: the exact auto-configured ChatModel bean names (commonly
 * "openAiChatModel" / "anthropicChatModel" / "vertexAiGeminiChatModel" as of Spring AI 1.0, but
 * confirm against the pinned spring-ai.version - bean-naming has shifted across milestones).
 */
@Configuration
public class AiProviderConfig {

    @Value("${askpranav.ai.provider:anthropic}")
    private String provider;

    @Bean
    public ChatClient chatClient(ConfigurableListableBeanFactory beanFactory) {
        String beanName = switch (provider.toLowerCase()) {
            case "openai" -> "openAiChatModel";
            case "gemini", "vertex", "vertex-ai-gemini" -> "vertexAiGeminiChatModel";
            case "anthropic" -> "anthropicChatModel";
            default -> throw new IllegalStateException(
                    "Unknown askpranav.ai.provider '" + provider + "'. Expected one of: openai, anthropic, gemini");
        };
        ChatModel chatModel = (ChatModel) beanFactory.getBean(beanName);
        return ChatClient.builder(chatModel).build();
    }
}
