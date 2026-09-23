package com.askpranav.ai.orchestration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * First stage of the pipeline: a small, cheap, structured-output LLM call that classifies the
 * question into a route rather than always paying for both vector search and tool-binding on every
 * question. This explicit decide-then-route step is the orchestrator's actual value-add - Spring
 * AI's ChatClient already auto-invokes registered tools within a single call, so the interesting
 * part here isn't reimplementing tool-calling, it's deciding whether this question needs retrieval,
 * a tool call, or both, before spending tokens on either.
 */
@Component
public class PlannerAgent {

    private final ChatClient chatClient;

    public PlannerAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public PlanDecision decide(String question) {
        String prompt = """
                Classify how this question about Pranav's career should be answered. Respond with:
                - RAG_SEARCH: if it's a narrative/exploratory question best answered by semantic search
                  over his resume, project write-ups, and READMEs (e.g. "tell me about his fintech work").
                - TOOL_CALL: if it asks for an exact, structured, enumerable fact best answered by a
                  direct data lookup (e.g. "list his certifications", "what's his email").
                - BOTH: if it requires combining retrieved narrative context with structured tool data -
                  this is almost always the right choice when a job description is being matched against
                  his background.

                Question: %s
                """.formatted(question);

        // VERIFY: ChatClient.call().entity(Class) is the current structured-output shape for the
        // pinned Spring AI version - confirm against docs if this doesn't compile as-is.
        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(PlanDecision.class);
    }
}
