package com.askpranav.ai;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class SystemPromptProviderTest {

    @Test
    void loadsNonEmptyGuardrailedSystemPrompt() {
        SystemPromptProvider provider = new SystemPromptProvider(new ClassPathResource("prompts/system-prompt.st"));

        String prompt = provider.get();

        assertThat(prompt).isNotBlank();
        assertThat(prompt).containsIgnoringCase("never");
        assertThat(prompt).containsIgnoringCase("fabricate");
    }
}
