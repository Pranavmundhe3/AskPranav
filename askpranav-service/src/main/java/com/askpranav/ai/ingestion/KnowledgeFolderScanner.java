package com.askpranav.ai.ingestion;

import com.askpranav.domain.KnowledgeFile;
import com.askpranav.repository.KnowledgeFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Watches the knowledge folder while the app runs, so a document dropped in (or edited, or removed) is
 * reflected without a restart:
 * <ul>
 *   <li><b>Chat:</b> a new or changed file has its old chunks replaced by freshly embedded ones; a deleted
 *       file's chunks are removed.</li>
 *   <li><b>Database and site:</b> a resume-type document (see {@link KnowledgeFolderLoader#isResumeCandidate})
 *       is also read by the model into structured data and imported via {@link ResumeImportService}; the
 *       pages read those tables, so the change shows the next time a page loads.</li>
 * </ul>
 * Files are compared by SHA-256, remembered in the {@code KNOWLEDGE_FILE} table, so nothing is redone after
 * a restart unless the file changed. A file still being copied (modified moments ago) is left for the next
 * round. Failures (for example the model's quota) are recorded and retried after a pause, not every round.
 *
 * <p>The first scan after the table is empty (first run of this feature) only records the files that are
 * already there: the database was seeded from that same resume, so re-extracting it would spend model calls
 * to reproduce what is already stored. Anything that appears afterwards is processed normally.
 */
@Component
public class KnowledgeFolderScanner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeFolderScanner.class);

    private final KnowledgeFolderLoader loader;
    private final KnowledgeFileRepository files;
    private final VectorStoreMaintenance maintenance;
    private final VectorStore vectorStore;
    private final ResumeExtractor extractor;
    private final ResumeImportService importService;
    private final EntityKnowledgeIngestor entityIngestor;
    private final Duration settle;
    private final Duration retryAfter;
    private final Clock clock;

    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private volatile boolean enabled;
    private boolean firstScanDone;
    /** Set after a resume import; cleared once the row-derived chunks in the vector store are rebuilt. */
    private boolean entityChunksStale;

    @Autowired
    public KnowledgeFolderScanner(KnowledgeFolderLoader loader,
                                  KnowledgeFileRepository files,
                                  VectorStoreMaintenance maintenance,
                                  VectorStore vectorStore,
                                  ResumeExtractor extractor,
                                  ResumeImportService importService,
                                  EntityKnowledgeIngestor entityIngestor,
                                  @Value("${askpranav.knowledge.settle-ms:2000}") long settleMs,
                                  @Value("${askpranav.knowledge.retry-minutes:10}") long retryMinutes) {
        this(loader, files, maintenance, vectorStore, extractor, importService, entityIngestor,
                Duration.ofMillis(settleMs), Duration.ofMinutes(retryMinutes), Clock.systemUTC());
    }

    KnowledgeFolderScanner(KnowledgeFolderLoader loader, KnowledgeFileRepository files,
                           VectorStoreMaintenance maintenance, VectorStore vectorStore,
                           ResumeExtractor extractor, ResumeImportService importService,
                           EntityKnowledgeIngestor entityIngestor,
                           Duration settle, Duration retryAfter, Clock clock) {
        this.loader = loader;
        this.files = files;
        this.maintenance = maintenance;
        this.vectorStore = vectorStore;
        this.extractor = extractor;
        this.importService = importService;
        this.entityIngestor = entityIngestor;
        this.settle = settle;
        this.retryAfter = retryAfter;
        this.clock = clock;
    }

    /** Called once startup ingestion is done, so the timer never races it. */
    public void enable() {
        this.enabled = true;
    }

    @Scheduled(fixedDelayString = "${askpranav.knowledge.scan-interval-ms:10000}")
    public void scheduledScan() {
        if (enabled) {
            scan();
        }
    }

    /** One pass over the folder. Safe to call from anywhere; overlapping calls are skipped. */
    public void scan() {
        if (!scanning.compareAndSet(false, true)) {
            return;
        }
        try {
            doScan();
        } catch (Exception e) {
            log.error("Knowledge folder scan failed: {}", e.getMessage(), e);
        } finally {
            scanning.set(false);
        }
    }

    private void doScan() {
        boolean baseline = !firstScanDone && files.count() == 0;
        firstScanDone = true;

        List<Path> present = loader.listFiles();
        for (Path file : present) {
            process(file, baseline);
        }
        removeDeleted(present);

        if (entityChunksStale) {
            try {
                entityIngestor.refresh();
                entityChunksStale = false;
                log.info("Rebuilt the chat index of the structured resume data.");
            } catch (RuntimeException e) {
                log.warn("Could not rebuild the chat index of the resume data yet (will retry): {}", e.getMessage());
            }
        }
    }

    private void process(Path file, boolean baseline) {
        String name = file.getFileName().toString();
        Instant now = Instant.now(clock);
        try {
            if (Duration.between(Files.getLastModifiedTime(file).toInstant(), now).compareTo(settle) < 0) {
                return; // probably still being copied; look again next round
            }
            String hash = sha256(file);
            KnowledgeFile record = files.findByFileName(name).orElseGet(() -> new KnowledgeFile(name));

            boolean needEmbed = !hash.equals(record.getEmbeddedSha256()) || maintenance.countByFileName(name) == 0;
            boolean needImport = loader.isResumeCandidate(name) && !hash.equals(record.getImportedSha256());
            if (!needEmbed && !needImport) {
                return;
            }
            if (recentlyFailed(record, hash, now)) {
                return;
            }

            record.setSha256(hash);
            record.setLastAttemptAt(now);
            List<String> problems = new java.util.ArrayList<>();

            List<Document> documents = loader.read(file);
            if (needEmbed) {
                try {
                    embed(name, documents);
                    record.setEmbeddedSha256(hash);
                    log.info("Knowledge file '{}' embedded into the chat index.", name);
                } catch (RuntimeException e) {
                    problems.add("embedding failed: " + e.getMessage());
                }
            }
            if (needImport) {
                if (baseline) {
                    record.setImportedSha256(hash);
                    log.info("Knowledge file '{}' recorded as already imported (first scan).", name);
                } else {
                    try {
                        ResumeData data = extractor.extract(joinText(documents));
                        importService.apply(data);
                        record.setImportedSha256(hash);
                        entityChunksStale = true;
                        log.info("Resume '{}' imported into the database; the site shows it on the next page load.", name);
                    } catch (RuntimeException e) {
                        problems.add("resume import failed: " + e.getMessage());
                    }
                }
            }
            record.setLastError(problems.isEmpty() ? null : cut(String.join("; ", problems), 1000));
            files.save(record);
            problems.forEach(problem -> log.warn("Knowledge file '{}': {} (will retry after {} min)",
                    name, problem, retryAfter.toMinutes()));
        } catch (Exception e) {
            log.warn("Could not process knowledge file '{}': {}", name, e.getMessage());
        }
    }

    private boolean recentlyFailed(KnowledgeFile record, String hash, Instant now) {
        return record.getLastError() != null
                && hash.equals(record.getSha256())
                && record.getLastAttemptAt() != null
                && Duration.between(record.getLastAttemptAt(), now).compareTo(retryAfter) < 0;
    }

    private void embed(String fileName, List<Document> documents) {
        List<Document> chunks = new TokenTextSplitter().apply(documents);
        maintenance.deleteByFileName(fileName);
        if (!chunks.isEmpty()) {
            vectorStore.add(chunks);
        }
    }

    private void removeDeleted(List<Path> present) {
        if (!Files.isDirectory(loader.directory())) {
            return; // an unreachable folder is not the same as an empty one; do not wipe the index
        }
        Set<String> names = present.stream().map(p -> p.getFileName().toString()).collect(Collectors.toSet());
        for (KnowledgeFile record : files.findAll()) {
            if (!names.contains(record.getFileName())) {
                int removed = maintenance.deleteByFileName(record.getFileName());
                files.delete(record);
                log.info("Knowledge file '{}' was removed; dropped {} chunk(s) from the chat index. "
                        + "Database rows imported from it are kept.", record.getFileName(), removed);
            }
        }
    }

    private static String joinText(List<Document> documents) {
        return documents.stream().map(Document::getText).collect(Collectors.joining("\n\n"));
    }

    static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String cut(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
