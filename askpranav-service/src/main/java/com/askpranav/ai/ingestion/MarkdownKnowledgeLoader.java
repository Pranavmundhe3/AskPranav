package com.askpranav.ai.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads free-text narrative content - the full resume, a longer "about me" bio, project write-ups -
 * from {@code src/main/resources/knowledge/*.md}. This is the secondary/enrichment source alongside
 * the JPA-entity-derived documents: it exists for content that doesn't fit the rigid 6/7-entity
 * schema (a resume reads very differently from a database row).
 */
@Component
public class MarkdownKnowledgeLoader {

    private static final Logger log = LoggerFactory.getLogger(MarkdownKnowledgeLoader.class);
    private static final String KNOWLEDGE_DIR = "classpath:knowledge/";
    private static final String[] KNOWN_FILES = {"resume.md", "about.md"};

    private final ResourceLoader resourceLoader;

    public MarkdownKnowledgeLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public List<Document> loadAll() {
        List<Document> documents = new ArrayList<>();
        for (String fileName : KNOWN_FILES) {
            Resource resource = resourceLoader.getResource(KNOWLEDGE_DIR + fileName);
            if (!resource.exists()) {
                continue;
            }
            try {
                String content = new String(resource.getContentAsByteArray(), StandardCharsets.UTF_8);
                documents.add(Document.builder()
                        .text(content)
                        .metadata(Map.of("source", "markdown", "fileName", fileName, "label", fileName))
                        .build());
            } catch (IOException e) {
                log.warn("Could not read knowledge file {}: {}", fileName, e.getMessage());
            }
        }
        return documents;
    }
}
