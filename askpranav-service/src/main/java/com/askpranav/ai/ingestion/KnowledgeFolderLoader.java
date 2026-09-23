package com.askpranav.ai.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Loads free-text narrative content - resume, "about me" bio, project write-ups - from every file
 * dropped into {@code src/main/resources/knowledge/}. Plain text files (.md, .txt) are read verbatim;
 * everything else (.docx, .pdf, .pptx, ...) is parsed via Apache Tika. This is the secondary/
 * enrichment source alongside the JPA-entity-derived documents: it exists for content that doesn't
 * fit the rigid 6/7-entity schema (a resume reads very differently from a database row).
 *
 * There's no file list to maintain here: drop a new/updated resume, bio, or write-up into the folder
 * and restart the app (set askpranav.ingestion.mode=always while actively editing content, since the
 * default if-empty mode skips re-ingestion once the vector store already has data) to pick it up.
 */
@Component
public class KnowledgeFolderLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeFolderLoader.class);
    private static final String KNOWLEDGE_DIR_PATTERN = "classpath*:knowledge/*";
    private static final Set<String> PLAIN_TEXT_EXTENSIONS = Set.of("md", "markdown", "txt");

    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    public List<Document> loadAll() {
        List<Document> documents = new ArrayList<>();
        Resource[] resources;
        try {
            resources = resourcePatternResolver.getResources(KNOWLEDGE_DIR_PATTERN);
        } catch (IOException e) {
            log.warn("Could not scan knowledge/ folder: {}", e.getMessage());
            return documents;
        }

        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            if (fileName == null || !resource.isReadable()) {
                continue;
            }
            try {
                if (PLAIN_TEXT_EXTENSIONS.contains(extension(fileName))) {
                    documents.add(readPlainText(resource, fileName));
                } else {
                    documents.addAll(readWithTika(resource, fileName));
                }
            } catch (Exception e) {
                log.warn("Skipping unreadable knowledge file {}: {}", fileName, e.getMessage());
            }
        }
        log.info("Loaded {} document(s) from knowledge/ folder.", documents.size());
        return documents;
    }

    private Document readPlainText(Resource resource, String fileName) throws IOException {
        String content = new String(resource.getContentAsByteArray(), StandardCharsets.UTF_8);
        return Document.builder()
                .text(content)
                .metadata("source", "knowledge-folder")
                .metadata("fileName", fileName)
                .metadata("label", fileName)
                .build();
    }

    private List<Document> readWithTika(Resource resource, String fileName) {
        List<Document> parsed = new TikaDocumentReader(resource).get();
        List<Document> tagged = new ArrayList<>(parsed.size());
        for (Document doc : parsed) {
            tagged.add(doc.mutate()
                    .metadata("source", "knowledge-folder")
                    .metadata("fileName", fileName)
                    .metadata("label", fileName)
                    .build());
        }
        return tagged;
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
