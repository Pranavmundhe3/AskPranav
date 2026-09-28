package com.askpranav.ai.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Ingests GitHub READMEs. Each configured entry (askpranav.knowledge.github-repos, comma separated) may be:
 * <ul>
 *   <li>a repository: {@code owner/repo} or {@code https://github.com/owner/repo}</li>
 *   <li>an account: {@code https://github.com/owner} (or just {@code owner}) - every public, non-fork
 *       repository of that account is used, up to {@value #MAX_REPOS_PER_ACCOUNT}</li>
 * </ul>
 * READMEs come from the GitHub REST API (which resolves the default branch itself). Public data needs no
 * token, but unauthenticated calls are limited to 60 per hour, and an account costs one call for the
 * listing plus one per repository.
 */
@Component
public class GithubReadmeFetcher {

    private static final Logger log = LoggerFactory.getLogger(GithubReadmeFetcher.class);

    static final int MAX_REPOS_PER_ACCOUNT = 30;
    private static final Pattern VALID_ENTRY = Pattern.compile("[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)?");
    private static final MediaType GITHUB_JSON = MediaType.valueOf("application/vnd.github+json");
    private static final MediaType GITHUB_RAW = MediaType.valueOf("application/vnd.github.raw+json");

    private final RestClient restClient;
    private final List<String> entries;

    public GithubReadmeFetcher(@Value("${askpranav.knowledge.github-repos:}") String configured) {
        this.restClient = RestClient.builder().baseUrl("https://api.github.com").build();
        this.entries = parseEntries(configured);
    }

    /** Turns the configured comma-separated value into normalized {@code owner} / {@code owner/repo} entries. */
    static List<String> parseEntries(String configured) {
        if (configured == null || configured.isBlank()) {
            return List.of();
        }
        return Arrays.stream(configured.split(","))
                .map(GithubReadmeFetcher::normalize)
                .flatMap(Optional::stream)
                .toList();
    }

    static Optional<String> normalize(String raw) {
        String value = raw == null ? "" : raw.trim();
        value = value.replaceFirst("(?i)^https?://(www\\.)?github\\.com/", "")
                .replaceFirst("(?i)^github\\.com/", "");
        String[] segments = value.split("/");
        value = segments.length >= 2 && !segments[1].isBlank() ? segments[0] + "/" + segments[1] : segments[0];
        value = value.replaceFirst("(?i)\\.git$", "");
        boolean dotOnlyPart = Arrays.stream(value.split("/")).anyMatch(part -> part.matches("\\.+"));
        if (value.isBlank() || dotOnlyPart || !VALID_ENTRY.matcher(value).matches()) {
            if (!value.isBlank()) {
                log.warn("Ignoring unrecognised GitHub entry '{}'", raw);
            }
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public List<Document> fetchAll() {
        List<Document> documents = new ArrayList<>();
        for (String entry : entries) {
            List<String> repos = entry.contains("/") ? List.of(entry) : listPublicRepos(entry);
            for (String repo : repos) {
                fetchReadme(repo).ifPresent(documents::add);
            }
        }
        return documents;
    }

    private List<String> listPublicRepos(String owner) {
        try {
            List<Map<String, Object>> repos = restClient.get()
                    .uri("/users/{owner}/repos?type=owner&sort=pushed&per_page=100", owner)
                    .accept(GITHUB_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() { });
            if (repos == null) {
                return List.of();
            }
            return repos.stream()
                    .filter(repo -> !Boolean.TRUE.equals(repo.get("fork")))
                    .map(repo -> String.valueOf(repo.get("full_name")))
                    .limit(MAX_REPOS_PER_ACCOUNT)
                    .toList();
        } catch (Exception e) {
            log.warn("Could not list GitHub repositories for {}: {}", owner, e.getMessage());
            return List.of();
        }
    }

    private Optional<Document> fetchReadme(String ownerRepoSlug) {
        try {
            // Separate variables: a "/" inside one URI variable would be percent-encoded.
            String[] parts = ownerRepoSlug.split("/", 2);
            String content = restClient.get()
                    .uri("/repos/{owner}/{repo}/readme", parts[0], parts[1])
                    .accept(GITHUB_RAW)
                    .retrieve()
                    .body(String.class);

            if (content == null || content.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Document.builder()
                    .text(content)
                    .metadata(Map.of("source", "github-readme", "repo", ownerRepoSlug, "label", ownerRepoSlug + " README"))
                    .build());
        } catch (Exception e) {
            log.warn("Could not fetch README for {}: {}", ownerRepoSlug, e.getMessage());
            return Optional.empty();
        }
    }
}
