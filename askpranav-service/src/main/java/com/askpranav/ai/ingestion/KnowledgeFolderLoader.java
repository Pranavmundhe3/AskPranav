package com.askpranav.ai.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads narrative content - resume, "about me" bio, project write-ups - from a real folder on disk
 * (askpranav.knowledge.dir). It is a plain directory rather than the packaged classpath so that files
 * dropped in while the app runs can be seen; {@link KnowledgeFolderScanner} does the watching.
 * Plain text files (.md, .txt) are read verbatim; PDF, Word, PowerPoint and similar go through Apache Tika.
 */
@Component
public class KnowledgeFolderLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeFolderLoader.class);

    private static final Set<String> PLAIN_TEXT = Set.of("md", "markdown", "txt");
    private static final Set<String> TIKA_FORMATS = Set.of("pdf", "doc", "docx", "odt", "rtf", "ppt", "pptx");
    /** Document formats a person would keep a resume in; a note like resume.md is knowledge, not a resume. */
    private static final Set<String> RESUME_FORMATS = Set.of("pdf", "doc", "docx", "odt", "rtf");

    private final Path directory;
    private final List<String> resumeNameHints;

    public KnowledgeFolderLoader(
            @Value("${askpranav.knowledge.dir:src/main/resources/knowledge}") String directory,
            @Value("${askpranav.knowledge.resume-name-hints:resume,cv,lebenslauf}") String resumeNameHints) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.resumeNameHints = Arrays.stream(resumeNameHints.split(","))
                .map(hint -> hint.trim().toLowerCase(Locale.ROOT))
                .filter(hint -> !hint.isEmpty())
                .toList();
    }

    public Path directory() {
        return directory;
    }

    /** Supported files directly inside the folder, sorted by name. Office lock/temp files are ignored. */
    public List<Path> listFiles() {
        if (!Files.isDirectory(directory)) {
            log.warn("Knowledge folder {} does not exist; set ASKPRANAV_KNOWLEDGE_DIR to the folder to watch.", directory);
            return List.of();
        }
        try (Stream<Path> entries = Files.list(directory)) {
            return entries
                    .filter(Files::isRegularFile)
                    .filter(path -> isSupported(path.getFileName().toString()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            log.warn("Could not list knowledge folder {}: {}", directory, e.getMessage());
            return List.of();
        }
    }

    boolean isSupported(String fileName) {
        // "~$x.docx" is the lock file Word keeps next to an open document; dotfiles are editor/OS noise.
        if (fileName.startsWith("~$") || fileName.startsWith(".")) {
            return false;
        }
        String extension = extension(fileName);
        return PLAIN_TEXT.contains(extension) || TIKA_FORMATS.contains(extension);
    }

    /** True for a PDF/Word-style document whose name suggests it is a resume (resume, cv, lebenslauf). */
    public boolean isResumeCandidate(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return RESUME_FORMATS.contains(extension(fileName)) && resumeNameHints.stream().anyMatch(lower::contains);
    }

    /** Reads one file into documents tagged with its name, so its chunks can later be replaced or removed. */
    public List<Document> read(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        if (PLAIN_TEXT.contains(extension(fileName))) {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            return List.of(Document.builder()
                    .text(content)
                    .metadata("source", "knowledge-folder")
                    .metadata("fileName", fileName)
                    .metadata("label", fileName)
                    .build());
        }
        List<Document> parsed = new TikaDocumentReader(new FileSystemResource(file)).get();
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

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
