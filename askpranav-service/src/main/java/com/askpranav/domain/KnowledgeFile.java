package com.askpranav.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Bookkeeping for one file in the knowledge folder: which version (SHA-256) has been embedded into the
 * vector store and which has been imported into the database, so a restart or a rescan only redoes work
 * when the file actually changed.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "KNOWLEDGE_FILE")
public class KnowledgeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "FILE_NAME", unique = true, nullable = false, length = 500)
    private String fileName;

    /** Hash of the file version last attempted (used to avoid hammering a file that keeps failing). */
    @Column(name = "SHA256", length = 64)
    private String sha256;

    @Column(name = "EMBEDDED_SHA256", length = 64)
    private String embeddedSha256;

    @Column(name = "IMPORTED_SHA256", length = 64)
    private String importedSha256;

    @Column(name = "LAST_ERROR", length = 1000)
    private String lastError;

    @Column(name = "LAST_ATTEMPT_AT")
    private Instant lastAttemptAt;

    public KnowledgeFile(String fileName) {
        this.fileName = fileName;
    }
}
