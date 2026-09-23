package com.askpranav.ai.tools.dto;

import java.util.List;

/**
 * Result of {@code matchJobDescription}. Deliberately NOT a finished verdict ("he's a good fit") -
 * this is grounded evidence pulled from the vector store using the job description text itself as
 * the search query. The LLM turn that invoked the tool synthesizes the actual comparison from this
 * evidence, per the system prompt's no-fabrication guardrail.
 */
public record JobMatchEvidenceDto(
        String jobDescriptionExcerpt,
        List<MatchedSnippet> matchedEvidence
) {
    public record MatchedSnippet(String sourceType, String sourceLabel, String excerpt) {
    }
}
