package com.askpranav.ai.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Ingests the literal "GitHub READMEs" requirement: for each configured "owner/repo" slug
 * (askpranav.knowledge.github-repos), fetches that repo's README via the GitHub Contents API
 * (which resolves the default branch automatically - no need to guess main vs master vs HEAD) and
 * turns it into a Document tagged source=github-readme. Public repos need no auth; if GitHub's
 * unauthenticated rate limit becomes a problem, a GITHUB_TOKEN can be added to the Authorization
 * header later.
 */
@Component
public class GithubReadmeFetcher {

    private static final Logger log = LoggerFactory.getLogger(GithubReadmeFetcher.class);

    private final RestClient restClient;
    private final List<String> repoSlugs;

    public GithubReadmeFetcher(@Value("${askpranav.knowledge.github-repos:}") String repoSlugsCsv) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.raw+json")
                .build();
        this.repoSlugs = repoSlugsCsv == null || repoSlugsCsv.isBlank()
                ? List.of()
                : Arrays.stream(repoSlugsCsv.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    public List<Document> fetchAll() {
        List<Document> documents = new ArrayList<>();
        for (String slug : repoSlugs) {
            fetchReadme(slug).ifPresent(documents::add);
        }
        return documents;
    }

    private java.util.Optional<Document> fetchReadme(String ownerRepoSlug) {
        try {
            String content = restClient.get()
                    .uri("/repos/{slug}/readme", ownerRepoSlug)
                    .accept(MediaType.valueOf("application/vnd.github.raw+json"))
                    .retrieve()
                    .body(String.class);

            if (content == null || content.isBlank()) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(Document.builder()
                    .text(content)
                    .metadata(Map.of("source", "github-readme", "repo", ownerRepoSlug, "label", ownerRepoSlug + " README"))
                    .build());
        } catch (Exception e) {
            log.warn("Could not fetch README for {}: {}", ownerRepoSlug, e.getMessage());
            return java.util.Optional.empty();
        }
    }
}
