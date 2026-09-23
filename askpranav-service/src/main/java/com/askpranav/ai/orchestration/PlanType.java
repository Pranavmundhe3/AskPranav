package com.askpranav.ai.orchestration;

/**
 * NOT a LangGraph-style graph - Java has no LangGraph equivalent. This is a hand-rolled linear
 * pipeline (Planner -> Retriever/ToolCaller -> Writer) that demonstrates the same
 * plan-then-execute-then-synthesize concept LangGraph formalizes in Python.
 */
public enum PlanType {
    /** Pure vector-search-grounded narrative question (e.g. "tell me about his fintech work"). */
    RAG_SEARCH,
    /** Better answered by an exact, structured tool call (e.g. "list all his certifications"). */
    TOOL_CALL,
    /** Needs both - e.g. matching a pasted job description against retrieved + tool-fetched facts. */
    BOTH
}
