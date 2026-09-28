package com.askpranav.repository;

import com.askpranav.domain.KnowledgeFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeFileRepository extends JpaRepository<KnowledgeFile, Long> {

    Optional<KnowledgeFile> findByFileName(String fileName);
}
