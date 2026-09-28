package com.askpranav.ai.ingestion;

import com.askpranav.domain.KnowledgeFile;
import com.askpranav.repository.KnowledgeFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Exercises the folder watcher with a real temp folder, a controllable clock and in-memory stand-ins. */
class KnowledgeFolderScannerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T12:00:00Z");

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock(T0);
    private final Map<String, KnowledgeFile> store = new HashMap<>();
    private final Set<String> embedded = new HashSet<>();

    private final KnowledgeFileRepository files = mock(KnowledgeFileRepository.class);
    private final VectorStoreMaintenance maintenance = mock(VectorStoreMaintenance.class);
    private final VectorStore vectorStore = mock(VectorStore.class);
    private final ResumeExtractor extractor = mock(ResumeExtractor.class);
    private final ResumeImportService importService = mock(ResumeImportService.class);
    private final EntityKnowledgeIngestor entityIngestor = mock(EntityKnowledgeIngestor.class);

    private KnowledgeFolderLoader loader;
    private KnowledgeFolderScanner scanner;

    @BeforeEach
    void setUp() {
        loader = spy(new KnowledgeFolderLoader(dir.toString(), "resume,cv"));
        scanner = newScanner(loader);
        scanner.enable();

        when(files.findByFileName(anyString())).thenAnswer(inv -> Optional.ofNullable(store.get(inv.<String>getArgument(0))));
        when(files.save(any(KnowledgeFile.class))).thenAnswer(inv -> {
            KnowledgeFile file = inv.getArgument(0);
            store.put(file.getFileName(), file);
            return file;
        });
        when(files.findAll()).thenAnswer(inv -> new ArrayList<>(store.values()));
        when(files.count()).thenAnswer(inv -> (long) store.size());
        doAnswer(inv -> {
            store.remove(inv.<KnowledgeFile>getArgument(0).getFileName());
            return null;
        }).when(files).delete(any(KnowledgeFile.class));

        when(maintenance.countByFileName(anyString())).thenAnswer(inv -> embedded.contains(inv.<String>getArgument(0)) ? 1 : 0);
        when(maintenance.deleteByFileName(anyString())).thenAnswer(inv -> embedded.remove(inv.<String>getArgument(0)) ? 1 : 0);
        doAnswer(inv -> {
            List<Document> chunks = inv.getArgument(0);
            chunks.forEach(chunk -> embedded.add((String) chunk.getMetadata().get("fileName")));
            return null;
        }).when(vectorStore).add(anyList());
    }

    private KnowledgeFolderScanner newScanner(KnowledgeFolderLoader forLoader) {
        return new KnowledgeFolderScanner(forLoader, files, maintenance, vectorStore, extractor, importService,
                entityIngestor, Duration.ofSeconds(2), Duration.ofMinutes(10), clock);
    }

    private Path write(String name, String content, long ageSeconds) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content);
        Files.setLastModifiedTime(file, FileTime.from(clock.instant().minusSeconds(ageSeconds)));
        return file;
    }

    /** A "resume" whose text is supplied directly, since building a real PDF is not what is under test. */
    private void writeResume(String name, String text) throws IOException {
        Path file = write(name, "binary-placeholder-" + text, 60);
        doReturn(List.of(Document.builder().text(text).metadata("fileName", name).build())).when(loader).read(file);
    }

    @Test
    void aNewTextFileIsEmbeddedOnceAndNeverSentToTheModel() throws Exception {
        write("notes.md", "Notes about Kafka and payments", 60);

        scanner.scan();
        scanner.scan();

        verify(vectorStore, times(1)).add(anyList());
        assertThat(store.get("notes.md").getEmbeddedSha256()).isNotNull();
        verifyNoInteractions(extractor, importService);
    }

    @Test
    void aChangedFileHasItsOldChunksReplaced() throws Exception {
        write("notes.md", "first version", 60);
        scanner.scan();

        write("notes.md", "second version", 60);
        scanner.scan();

        verify(maintenance, times(2)).deleteByFileName("notes.md");
        verify(vectorStore, times(2)).add(anyList());
    }

    @Test
    void aDeletedFileDisappearsFromTheChatIndexAndTheRecords() throws Exception {
        Path file = write("notes.md", "temporary", 60);
        scanner.scan();
        assertThat(embedded).contains("notes.md");

        Files.delete(file);
        scanner.scan();

        assertThat(embedded).doesNotContain("notes.md");
        assertThat(store).doesNotContainKey("notes.md");
    }

    @Test
    void aFileStillBeingCopiedWaitsForTheNextRound() throws Exception {
        write("big.md", "still copying", 0);

        scanner.scan();
        verify(vectorStore, never()).add(anyList());

        clock.advance(Duration.ofSeconds(5));
        scanner.scan();
        verify(vectorStore, times(1)).add(anyList());
    }

    @Test
    void chunksThatWentMissingFromTheStoreAreRestoredEvenIfTheFileIsUnchanged() throws Exception {
        write("notes.md", "keep me searchable", 60);
        scanner.scan();
        embedded.clear(); // someone emptied the vector table

        scanner.scan();

        verify(vectorStore, times(2)).add(anyList());
    }

    @Test
    void theFirstScanOnlyRecordsAResumeThatWasAlreadyThere() throws Exception {
        writeResume("my-cv.pdf", "existing resume text");

        scanner.scan();

        verifyNoInteractions(extractor, importService);
        assertThat(store.get("my-cv.pdf").getImportedSha256()).isNotNull();
        assertThat(embedded).contains("my-cv.pdf"); // still searchable in the chat
    }

    @Test
    void aResumeAddedLaterIsImportedAndTheChatIndexOfTheRowsRefreshed() throws Exception {
        write("notes.md", "first scan happens here", 60);
        scanner.scan();
        ResumeData data = new ResumeData("Summary.", List.of(), null, null, null, null, null, null);
        when(extractor.extract(anyString())).thenReturn(data);

        writeResume("new-cv.pdf", "brand new resume text");
        scanner.scan();

        verify(extractor).extract("brand new resume text");
        verify(importService).apply(data);
        verify(entityIngestor).refresh();
        assertThat(store.get("new-cv.pdf").getImportedSha256()).isNotNull();
        assertThat(store.get("new-cv.pdf").getLastError()).isNull();
    }

    @Test
    void aFailedImportKeepsTheDatabaseUntouchedAndIsRetriedOnlyAfterThePause() throws Exception {
        write("notes.md", "first scan happens here", 60);
        scanner.scan();
        when(extractor.extract(anyString())).thenThrow(new RuntimeException("quota exceeded"));
        writeResume("new-cv.pdf", "resume text");

        scanner.scan();
        assertThat(store.get("new-cv.pdf").getLastError()).contains("resume import failed").contains("quota exceeded");
        assertThat(store.get("new-cv.pdf").getImportedSha256()).isNull();
        verify(extractor, times(1)).extract(anyString());
        verifyNoInteractions(importService);

        clock.advance(Duration.ofMinutes(1));
        scanner.scan();
        verify(extractor, times(1)).extract(anyString()); // still waiting

        clock.advance(Duration.ofMinutes(10));
        scanner.scan();
        verify(extractor, times(2)).extract(anyString()); // pause over
    }

    @Test
    void aDocumentThatIsNotNamedLikeAResumeNeverGoesToTheModel() throws Exception {
        write("notes.md", "first scan happens here", 60);
        scanner.scan();
        writeResume("project-writeup.pdf", "a project description");

        scanner.scan();

        verifyNoInteractions(extractor, importService);
        assertThat(embedded).contains("project-writeup.pdf");
    }

    @Test
    void anUnreachableFolderDoesNotWipeTheChatIndex() {
        store.put("old.md", new KnowledgeFile("old.md"));
        KnowledgeFolderScanner other = newScanner(new KnowledgeFolderLoader(dir.resolve("missing").toString(), "resume"));

        other.scan();

        assertThat(store).containsKey("old.md");
        verify(maintenance, never()).deleteByFileName(anyString());
    }

    @Test
    void theTimerDoesNothingUntilStartupHasHandedOver() throws Exception {
        KnowledgeFolderScanner fresh = newScanner(loader);
        write("notes.md", "waiting", 60);

        fresh.scheduledScan();
        verify(vectorStore, never()).add(anyList());

        fresh.enable();
        fresh.scheduledScan();
        verify(vectorStore, times(1)).add(anyList());
    }

    private static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
