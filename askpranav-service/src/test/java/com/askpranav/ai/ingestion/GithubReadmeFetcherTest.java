package com.askpranav.ai.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GithubReadmeFetcherTest {

    @Test
    void acceptsAnAccountUrl() {
        assertThat(GithubReadmeFetcher.normalize("https://github.com/Pranavmundhe3")).contains("Pranavmundhe3");
        assertThat(GithubReadmeFetcher.normalize("https://github.com/Pranavmundhe3/")).contains("Pranavmundhe3");
    }

    @Test
    void acceptsARepoInAnyCommonForm() {
        String expected = "Pranavmundhe3/AskPranav";
        assertThat(GithubReadmeFetcher.normalize("Pranavmundhe3/AskPranav")).contains(expected);
        assertThat(GithubReadmeFetcher.normalize("https://github.com/Pranavmundhe3/AskPranav")).contains(expected);
        assertThat(GithubReadmeFetcher.normalize("https://github.com/Pranavmundhe3/AskPranav.git")).contains(expected);
        assertThat(GithubReadmeFetcher.normalize("https://github.com/Pranavmundhe3/AskPranav/tree/main")).contains(expected);
        assertThat(GithubReadmeFetcher.normalize("github.com/Pranavmundhe3/AskPranav")).contains(expected);
    }

    @Test
    void rejectsAnythingThatCouldEscapeTheApiPath() {
        assertThat(GithubReadmeFetcher.normalize("bad owner!")).isEqualTo(Optional.empty());
        assertThat(GithubReadmeFetcher.normalize("../../etc/passwd")).isEqualTo(Optional.empty());
        assertThat(GithubReadmeFetcher.normalize("")).isEqualTo(Optional.empty());
    }

    @Test
    void parsesACommaSeparatedMixOfEntries() {
        List<String> entries = GithubReadmeFetcher.parseEntries(
                " https://github.com/Pranavmundhe3 , other/repo ,, ");

        assertThat(entries).containsExactly("Pranavmundhe3", "other/repo");
        assertThat(GithubReadmeFetcher.parseEntries(null)).isEmpty();
        assertThat(GithubReadmeFetcher.parseEntries("  ")).isEmpty();
    }
}
