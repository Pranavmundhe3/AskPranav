package com.askpranav.ai.ingestion;

import com.askpranav.repository.KnowledgeFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The "ingestion" step of the RAG pipeline at startup: the structured rows (summary, experience, skills,
 * ...) and any GitHub READMEs are chunked, embedded and stored in the pgvector-backed VectorStore that
 * {@link com.askpranav.ai.rag.RagQuestionService} and
 * {@link com.askpranav.ai.tools.BiographyTools#matchJobDescription} query.
 *
 * <p>The knowledge folder is not handled here: {@link KnowledgeFolderScanner} owns it, embeds each file
 * and keeps watching for changes while the app runs. This runner hands over to it when it is done.
 *
 * <p>askpranav.ingestion.mode=if-empty (default) skips the rows and READMEs once the store has data;
 * "always" empties the store (and forgets which knowledge files were embedded, so the scanner re-embeds
 * them) and rebuilds everything, so re-ingesting replaces the old chunks instead of duplicating them.
 * Rows edited later through the API reach the chat at the next "always" start.
 */
@Component
@Order(2)
public class KnowledgeBaseIngestionRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseIngestionRunner.class);

    private final EntityKnowledgeIngestor entityIngestor;
    private final GithubReadmeFetcher githubReadmeFetcher;
    private final VectorStore vectorStore;
    private final VectorStoreMaintenance maintenance;
    private final KnowledgeFileRepository knowledgeFiles;
    private final KnowledgeFolderScanner scanner;
    private final String ingestionMode;

    public KnowledgeBaseIngestionRunner(EntityKnowledgeIngestor entityIngestor,
                                        GithubReadmeFetcher githubReadmeFetcher,
                                        VectorStore vectorStore,
                                        VectorStoreMaintenance maintenance,
                                        KnowledgeFileRepository knowledgeFiles,
                                        KnowledgeFolderScanner scanner,
                                        @Value("${askpranav.ingestion.mode:if-empty}") String ingestionMode) {
        this.entityIngestor = entityIngestor;
        this.githubReadmeFetcher = githubReadmeFetcher;
        this.vectorStore = vectorStore;
        this.maintenance = maintenance;
        this.knowledgeFiles = knowledgeFiles;
        this.scanner = scanner;
        this.ingestionMode = ingestionMode;
    }

    @Override
    public void run(String... args) {
        try {
            ingestRowsAndReadmes();
        } finally {
            // Whatever happened above, the site must come up and the folder must be watched.
            scanner.enable();
            scanner.scan();
        }
    }

    private void ingestRowsAndReadmes() {
        boolean always = "always".equalsIgnoreCase(ingestionMode);
        if (!always && vectorStoreLooksPopulated()) {
            log.info("Vector store already populated and askpranav.ingestion.mode=if-empty; skipping re-ingestion.");
            return;
        }

        List<Document> documents = new ArrayList<>(entityIngestor.documents());
        documents.addAll(githubReadmeFetcher.fetchAll());
        if (documents.isEmpty()) {
            log.warn("No knowledge content found to ingest - RAG answers will have no grounded context yet.");
            return;
        }

        List<Document> chunks = new TokenTextSplitter().apply(documents);
        try {
            if (always) {
                int removed = maintenance.deleteAll();
                knowledgeFiles.deleteAll();
                log.info("Cleared {} existing chunks before re-ingesting.", removed);
            }
            vectorStore.add(chunks);
            log.info("Ingested {} source documents as {} chunks into the vector store.", documents.size(), chunks.size());
        } catch (RuntimeException e) {
            // Typically the embedding API refusing (quota, outage). The site and its data endpoints do not
            // need the vector store, so keep serving; a restart retries while the store is still empty.
            log.error("Ingestion failed, so chat answers have less context until it succeeds: {}", e.getMessage());
        }
    }

    private boolean vectorStoreLooksPopulated() {
        try {
            return !vectorStore.similaritySearch(SearchRequest.builder().query("pranav").topK(1).build()).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
