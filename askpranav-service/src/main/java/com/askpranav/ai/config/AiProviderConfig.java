package com.askpranav.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The model-agnostic layer: {@code askpranav.ai.provider} picks which of the auto-configured
 * ChatModel beans (OpenAI / Anthropic / Google GenAI - each provided by its own Spring AI starter on
 * the classpath) backs the single {@link ChatClient} the rest of the app depends on. Swapping providers
 * is a config change, not a code change - the point the "model-agnostic" requirement is asking for.
 *
 * Bean names verified against spring-ai 1.1.8's autoconfiguration: "openAiChatModel",
 * "anthropicChatModel", "googleGenAiChatModel". Note {@code spring.ai.model.chat} (application.yml)
 * also has to select the same starter, or the other starters' beans won't exist to look up - which
 * is why the accepted provider names below are the selector names.
 */
@Configuration
public class AiProviderConfig {

    @Value("${askpranav.ai.provider:anthropic}")
    private String provider;

    @Bean
    public ChatClient chatClient(ConfigurableListableBeanFactory beanFactory) {
        String beanName = switch (provider.toLowerCase()) {
            case "openai" -> "openAiChatModel";
            case "google-genai" -> "googleGenAiChatModel";
            case "anthropic" -> "anthropicChatModel";
            default -> throw new IllegalStateException(
                    "Unknown askpranav.ai.provider '" + provider + "'. Expected one of: openai, anthropic, google-genai");
        };
        ChatModel chatModel = (ChatModel) beanFactory.getBean(beanName);
        return ChatClient.builder(chatModel).build();
    }
}
