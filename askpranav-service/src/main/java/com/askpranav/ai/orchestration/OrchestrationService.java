package com.askpranav.ai.orchestration;

import com.askpranav.ai.rag.AnswerResponse;
import com.askpranav.ai.rag.SourceSnippet;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ties the pipeline together: Planner decides the route, Retriever and/or ToolCaller gather
 * evidence accordingly, Writer synthesizes the final answer. This is the linear Java stand-in for a
 * LangGraph-style multi-agent graph - see {@link PlanType} for why it's linear rather than a graph
 * engine. Typically costs 2 LLM calls (Planner + Writer/ToolCaller) rather than always doing both
 * retrieval and tool-binding on every question regardless of whether either is needed.
 */
@Service
public class OrchestrationService {

    private final PlannerAgent plannerAgent;
    private final RetrieverAgent retrieverAgent;
    private final ToolCallAgent toolCallAgent;
    private final WriterAgent writerAgent;

    public OrchestrationService(PlannerAgent plannerAgent, RetrieverAgent retrieverAgent,
                                 ToolCallAgent toolCallAgent, WriterAgent writerAgent) {
        this.plannerAgent = plannerAgent;
        this.retrieverAgent = retrieverAgent;
        this.toolCallAgent = toolCallAgent;
        this.writerAgent = writerAgent;
    }

    public AnswerResponse answer(String question) {
        AgentState state = AgentState.initial(question);

        PlanDecision decision = plannerAgent.decide(question);
        state = state.withPlan(decision.plan(), decision.reasoning());

        String finalAnswer;
        List<Document> retrievedDocuments = List.of();

        switch (state.plan()) {
            case TOOL_CALL -> finalAnswer = toolCallAgent.call(question);
            case RAG_SEARCH -> {
                retrievedDocuments = retrieverAgent.retrieve(question);
                finalAnswer = writerAgent.write(question, retrievedDocuments, false);
            }
            case BOTH -> {
                retrievedDocuments = retrieverAgent.retrieve(question);
                finalAnswer = writerAgent.write(question, retrievedDocuments, true);
            }
            default -> throw new IllegalStateException("PlannerAgent returned no plan for: " + question);
        }

        state = state.withRetrievedDocuments(retrievedDocuments).withFinalAnswer(finalAnswer);

        List<SourceSnippet> sources = retrievedDocuments.stream()
                .map(doc -> new SourceSnippet(
                        String.valueOf(doc.getMetadata().getOrDefault("source", "unknown")),
                        String.valueOf(doc.getMetadata().getOrDefault("label", doc.getId())),
                        truncate(doc.getText(), 240)))
                .toList();

        return new AnswerResponse(state.finalAnswer(), sources, state.plan().name());
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
