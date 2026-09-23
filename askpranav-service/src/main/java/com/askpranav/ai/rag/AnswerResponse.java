package com.askpranav.ai.rag;

import java.util.List;

public record AnswerResponse(
        String answer,
        List<SourceSnippet> sources,
        String planUsed
) {
}
