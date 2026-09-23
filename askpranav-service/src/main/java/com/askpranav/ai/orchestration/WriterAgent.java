package com.askpranav.ai.orchestration;

import com.askpranav.ai.SystemPromptProvider;
import com.askpranav.ai.tools.BiographyTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Final synthesis stage: takes whatever evidence the earlier stages gathered - retrieved documents,
 * optionally also live tools - and produces the answer actually returned to the user, under the same
 * guardrailed system prompt as every other path (no fabrication, cite sources, decline off-topic).
 */
@Component
public class WriterAgent {

    private final ChatClient chatClient;
    private final BiographyTools biographyTools;
    private final String systemPrompt;

    public WriterAgent(ChatClient chatClient, BiographyTools biographyTools, SystemPromptProvider systemPromptProvider) {
        this.chatClient = chatClient;
        this.biographyTools = biographyTools;
        this.systemPrompt = systemPromptProvider.get();
    }

    /**
     * @param bindTools whether to also let the model call BiographyTools while writing (used for the
     *                  BOTH route, e.g. job-description matching, which benefits from both retrieved
     *                  narrative context and exact structured lookups)
     */
    public String write(String question, List<Document> retrievedDocuments, boolean bindTools) {
        String context = retrievedDocuments.isEmpty()
                ? "(No matching context was found in the knowledge base for this question.)"
                : buildContextBlock(retrievedDocuments);

        String userPrompt = """
                Question: %s

                Retrieved context (grounded facts about Pranav - answer ONLY using this context and,
                where relevant, by calling one of your tools; never invent facts not present here or
                returned by a tool call):
                %s
                """.formatted(question, context);

        var spec = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt);
        if (bindTools) {
            spec = spec.tools(biographyTools);
        }
        return spec.call().content();
    }

    private String buildContextBlock(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        for (Document doc : documents) {
            sb.append("- [")
                    .append(doc.getMetadata().getOrDefault("label", doc.getId()))
                    .append("] ")
                    .append(doc.getText())
                    .append("\n");
        }
        return sb.toString();
    }
}
