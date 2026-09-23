package com.askpranav.ai.ingestion;

import com.askpranav.repository.CertificationRepository;
import com.askpranav.repository.EducationRepository;
import com.askpranav.repository.ExperienceRepository;
import com.askpranav.repository.ProjectRepository;
import com.askpranav.repository.PublicationRepository;
import com.askpranav.repository.SkillRepository;
import com.askpranav.repository.SummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The "ingestion" step of the RAG pipeline. Runs once at startup: pulls every structured bio/project
 * row plus any knowledge/-folder and GitHub-README content, chunks it, and upserts it into the pgvector-backed
 * VectorStore that {@link com.askpranav.ai.rag.RagQuestionService} and
 * {@link com.askpranav.ai.tools.BiographyTools#matchJobDescription} both query.
 *
 * Known limitation, documented rather than silently accepted: this only runs at startup, so an edit
 * made via the existing POST /save-* CRUD endpoints won't show up in answers until the app restarts.
 * askpranav.ingestion.mode=if-empty (default) skips re-embedding on every restart once the store has
 * been populated once; set it to "always" during content-editing sessions.
 */
@Component
@Order(2)
public class KnowledgeBaseIngestionRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseIngestionRunner.class);

    private final SummaryRepository summaryRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;
    private final ProjectRepository projectRepository;
    private final PublicationRepository publicationRepository;
    private final BiographyDocumentMapper documentMapper;
    private final KnowledgeFolderLoader knowledgeFolderLoader;
    private final GithubReadmeFetcher githubReadmeFetcher;
    private final VectorStore vectorStore;
    private final String ingestionMode;

    public KnowledgeBaseIngestionRunner(SummaryRepository summaryRepository,
                                         ExperienceRepository experienceRepository,
                                         EducationRepository educationRepository,
                                         SkillRepository skillRepository,
                                         CertificationRepository certificationRepository,
                                         ProjectRepository projectRepository,
                                         PublicationRepository publicationRepository,
                                         BiographyDocumentMapper documentMapper,
                                         KnowledgeFolderLoader knowledgeFolderLoader,
                                         GithubReadmeFetcher githubReadmeFetcher,
                                         VectorStore vectorStore,
                                         @Value("${askpranav.ingestion.mode:if-empty}") String ingestionMode) {
        this.summaryRepository = summaryRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.skillRepository = skillRepository;
        this.certificationRepository = certificationRepository;
        this.projectRepository = projectRepository;
        this.publicationRepository = publicationRepository;
        this.documentMapper = documentMapper;
        this.knowledgeFolderLoader = knowledgeFolderLoader;
        this.githubReadmeFetcher = githubReadmeFetcher;
        this.vectorStore = vectorStore;
        this.ingestionMode = ingestionMode;
    }

    @Override
    public void run(String... args) {
        if ("if-empty".equalsIgnoreCase(ingestionMode) && vectorStoreLooksPopulated()) {
            log.info("Vector store already populated and askpranav.ingestion.mode=if-empty; skipping re-ingestion.");
            return;
        }

        List<Document> documents = new ArrayList<>();
        documents.addAll(documentMapper.mapAll(
                summaryRepository.findAll(),
                experienceRepository.findAll(),
                educationRepository.findAll(),
                skillRepository.findAll(),
                certificationRepository.findAll(),
                projectRepository.findAll(),
                publicationRepository.findAll()));
        documents.addAll(knowledgeFolderLoader.loadAll());
        documents.addAll(githubReadmeFetcher.fetchAll());

        if (documents.isEmpty()) {
            log.warn("No knowledge content found to ingest - RAG answers will have no grounded context yet.");
            return;
        }

        List<Document> chunks = new TokenTextSplitter().apply(documents);
        vectorStore.add(chunks);
        log.info("Ingested {} source documents as {} chunks into the vector store.", documents.size(), chunks.size());
    }

    private boolean vectorStoreLooksPopulated() {
        // VERIFY: cheapest reliable "is this empty" probe for the pinned Spring AI VectorStore
        // implementation - similaritySearch with topK=1 against a generic probe query is a
        // reasonable default but a direct count query against the vector_store table is more precise
        // if PgVectorStore exposes one in the pinned version.
        try {
            return !vectorStore.similaritySearch(
                    org.springframework.ai.vectorstore.SearchRequest.builder().query("pranav").topK(1).build()
            ).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
