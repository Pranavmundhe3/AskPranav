package com.askpranav.ai.config;

import com.askpranav.ai.tools.BiographyTools;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the MCP surface: if a @Tool method is renamed or dropped, or the provider stops picking up
 * BiographyTools, MCP clients silently lose a capability. No Spring context, database or LLM needed.
 */
class McpToolConfigTest {

    @Test
    void exposesEveryBiographyToolOverMcp() {
        ToolCallback[] callbacks = new McpToolConfig()
                .biographyToolCallbacks(new BiographyTools())
                .getToolCallbacks();

        Set<String> names = Arrays.stream(callbacks)
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toSet());

        assertThat(names).containsExactlyInAnyOrder(
                "getCareerSummary", "getExperience", "getSkills", "getEducation", "getCertifications",
                "getPublications", "getProjectDetails", "getContactInfo", "matchJobDescription");
    }
}
