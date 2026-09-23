package com.askpranav.ai.config;

import com.askpranav.ai.tools.BiographyTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the exact same {@link BiographyTools} methods used for in-app tool calling as MCP tools.
 * With spring-ai-starter-mcp-server-webmvc on the classpath, any ToolCallbackProvider bean is picked
 * up and served over the MCP transport automatically - so an MCP client (Claude Desktop, Claude
 * Code, or any other MCP-speaking assistant) added as "AskPranav" can call getContactInfo,
 * getProjectDetails, matchJobDescription, etc. directly, without going through the /ask endpoint.
 *
 * VERIFY at build time: exact class name/package for the method-based tool callback provider and
 * the MCP transport config keys (Streamable HTTP is the current recommended transport) against the
 * Spring AI version pinned in pom.xml.
 */
@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider biographyToolCallbacks(BiographyTools biographyTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(biographyTools)
                .build();
    }
}
