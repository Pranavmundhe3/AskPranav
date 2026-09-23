package com.askpranav.ai.orchestration;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Carries the pipeline's state from Planner through to Writer. Kept as an explicit, inspectable
 * object (rather than implicit local variables threaded through method calls) so each stage's
 * contribution is visible - the closest Java analogue to a LangGraph node's state update.
 */
public record AgentState(
        String question,
        PlanType plan,
        String planReasoning,
        List<Document> retrievedDocuments,
        String finalAnswer
) {

    public static AgentState initial(String question) {
        return new AgentState(question, null, null, List.of(), null);
    }

    public AgentState withPlan(PlanType plan, String reasoning) {
        return new AgentState(question, plan, reasoning, retrievedDocuments, finalAnswer);
    }

    public AgentState withRetrievedDocuments(List<Document> documents) {
        return new AgentState(question, plan, planReasoning, documents, finalAnswer);
    }

    public AgentState withFinalAnswer(String answer) {
        return new AgentState(question, plan, planReasoning, retrievedDocuments, answer);
    }
}
