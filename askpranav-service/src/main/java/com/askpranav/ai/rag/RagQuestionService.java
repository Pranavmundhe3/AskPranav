package com.askpranav.ai.rag;

import com.askpranav.ai.SystemPromptProvider;
import com.askpranav.ai.tools.BiographyTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The "retrieval + generation" half of the RAG pipeline (ingestion lives in
 * {@link com.askpranav.ai.ingestion.KnowledgeBaseIngestionRunner}). Retrieves the most relevant
 * chunks for the question, builds a grounded prompt around them using the guardrailed system prompt,
 * registers {@link BiographyTools} so the model can also reach for a live tool call (e.g. an exact
 * "list all certifications" is better answered by getCertifications() than by vector search), and
 * returns the answer together with the sources it was grounded in.
 */
@Service
public class RagQuestionService {

    private static final int TOP_K = 6;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final BiographyTools biographyTools;
    private final String systemPrompt;

    public RagQuestionService(ChatClient chatClient,
                               VectorStore vectorStore,
                               BiographyTools biographyTools,
                               SystemPromptProvider systemPromptProvider) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.biographyTools = biographyTools;
        this.systemPrompt = systemPromptProvider.get();
    }

    public AnswerResponse answer(String question) {
        List<Document> retrieved = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(TOP_K).build());

        String context = retrieved.isEmpty()
                ? "(No matching context was found in the knowledge base for this question.)"
                : buildContextBlock(retrieved);

        String userPrompt = """
                Question: %s

                Retrieved context (grounded facts about Pranav - answer ONLY using this context and,
                where relevant, by calling one of your tools; never invent facts not present here or
                returned by a tool call):
                %s
                """.formatted(question, context);

        String answerText = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .tools(biographyTools)
                .call()
                .content();

        List<SourceSnippet> sources = retrieved.stream()
                .map(doc -> new SourceSnippet(
                        String.valueOf(doc.getMetadata().getOrDefault("source", "unknown")),
                        String.valueOf(doc.getMetadata().getOrDefault("label", doc.getId())),
                        truncate(doc.getText(), 240)))
                .toList();

        return new AnswerResponse(answerText, sources, "RAG_SEARCH");
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

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
