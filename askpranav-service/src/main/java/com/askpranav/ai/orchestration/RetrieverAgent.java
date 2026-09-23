package com.askpranav.ai.orchestration;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Vector-search-only stage. Deliberately thin - its only job is retrieval, kept separate from
 * synthesis (WriterAgent) so each stage of the pipeline does exactly one thing.
 */
@Component
public class RetrieverAgent {

    private static final int TOP_K = 6;

    private final VectorStore vectorStore;

    public RetrieverAgent(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieve(String question) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(TOP_K).build());
    }
}
