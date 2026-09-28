package com.askpranav.ai.ingestion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeFolderLoaderTest {

    @TempDir
    Path dir;

    private KnowledgeFolderLoader loader() {
        return new KnowledgeFolderLoader(dir.toString(), "resume, cv ,lebenslauf");
    }

    @Test
    void listsSupportedFilesAndIgnoresOfficeLockFilesDotfilesAndUnknownTypes() throws IOException {
        for (String name : List.of("notes.md", "cv.pdf", "~$cv.docx", ".hidden.md", "program.exe", "readme.txt")) {
            Files.writeString(dir.resolve(name), "x");
        }
        Files.createDirectory(dir.resolve("sub.md"));

        List<String> names = loader().listFiles().stream().map(p -> p.getFileName().toString()).toList();

        assertThat(names).containsExactly("cv.pdf", "notes.md", "readme.txt");
    }

    @Test
    void onlyDocumentFormatsWithAResumeHintCountAsResumes() {
        KnowledgeFolderLoader loader = loader();

        assertThat(loader.isResumeCandidate("Lebenslauf-Pranav.docx")).isTrue();
        assertThat(loader.isResumeCandidate("My_CV_2026.pdf")).isTrue();
        assertThat(loader.isResumeCandidate("resume.md")).isFalse();     // a note, not a resume document
        assertThat(loader.isResumeCandidate("project-writeup.pdf")).isFalse();
    }

    @Test
    void plainTextIsReadVerbatimAndTaggedWithItsFileName() throws IOException {
        Path file = dir.resolve("about.md");
        Files.writeString(file, "# About\nHello");

        List<Document> documents = loader().read(file);

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getText()).isEqualTo("# About\nHello");
        assertThat(documents.get(0).getMetadata()).containsEntry("fileName", "about.md").containsEntry("source", "knowledge-folder");
    }

    @Test
    void aMissingFolderIsAnEmptyListNotAnError() {
        KnowledgeFolderLoader loader = new KnowledgeFolderLoader(dir.resolve("nope").toString(), "resume");

        assertThat(loader.listFiles()).isEmpty();
    }
}
