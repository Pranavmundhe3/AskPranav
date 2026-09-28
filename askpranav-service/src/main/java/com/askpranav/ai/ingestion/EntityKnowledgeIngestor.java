package com.askpranav.ai.ingestion;

import com.askpranav.repository.CertificationRepository;
import com.askpranav.repository.EducationRepository;
import com.askpranav.repository.ExperienceRepository;
import com.askpranav.repository.ProjectRepository;
import com.askpranav.repository.PublicationRepository;
import com.askpranav.repository.SkillRepository;
import com.askpranav.repository.SummaryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns the structured rows (summary, experience, skills, ...) into vector-store documents. Used at
 * startup, and again after a resume import changes those rows so the chat does not keep answering from
 * the previous version.
 */
@Component
public class EntityKnowledgeIngestor {

    static final String SOURCE = "jpa-entity";

    private final SummaryRepository summaryRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;
    private final ProjectRepository projectRepository;
    private final PublicationRepository publicationRepository;
    private final BiographyDocumentMapper documentMapper;
    private final VectorStore vectorStore;
    private final VectorStoreMaintenance maintenance;

    public EntityKnowledgeIngestor(SummaryRepository summaryRepository,
                                   ExperienceRepository experienceRepository,
                                   EducationRepository educationRepository,
                                   SkillRepository skillRepository,
                                   CertificationRepository certificationRepository,
                                   ProjectRepository projectRepository,
                                   PublicationRepository publicationRepository,
                                   BiographyDocumentMapper documentMapper,
                                   VectorStore vectorStore,
                                   VectorStoreMaintenance maintenance) {
        this.summaryRepository = summaryRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.skillRepository = skillRepository;
        this.certificationRepository = certificationRepository;
        this.projectRepository = projectRepository;
        this.publicationRepository = publicationRepository;
        this.documentMapper = documentMapper;
        this.vectorStore = vectorStore;
        this.maintenance = maintenance;
    }

    /** The current rows as documents (not yet chunked). */
    public List<Document> documents() {
        return documentMapper.mapAll(
                summaryRepository.findAll(),
                experienceRepository.findAll(),
                educationRepository.findAll(),
                skillRepository.findAll(),
                certificationRepository.findAll(),
                projectRepository.findAll(),
                publicationRepository.findAll());
    }

    /** Replaces the row-derived chunks in the vector store with ones built from the current rows. */
    public void refresh() {
        List<Document> chunks = new TokenTextSplitter().apply(documents());
        if (chunks.isEmpty()) {
            return;
        }
        maintenance.deleteBySource(SOURCE);
        vectorStore.add(chunks);
    }
}
