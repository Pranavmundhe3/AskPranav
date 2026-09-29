package com.askpranav.config;

import com.askpranav.controller.ProjectController;
import com.askpranav.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The MCP endpoints (GET /sse, POST /mcp/message) are not registered in this slice (that comes from Spring
 * AI's MCP server auto-configuration, not a plain @Controller), so a request to them here never reaches a
 * handler - which is exactly what lets these tests isolate the security rule: 401 means Spring Security's
 * authorizeHttpRequests rejected the request before dispatch; anything else (typically 404) means it let
 * the request through to routing.
 */
@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "askpranav.security.admin-username=admin",
        "askpranav.security.admin-password=admin-secret",
        "askpranav.security.mcp-username=mcp",
        "askpranav.security.mcp-password=mcp-secret"
})
class SecurityConfigMcpTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void sseWithoutCredentialsIsRejected() throws Exception {
        mvc.perform(get("/sse")).andExpect(status().isUnauthorized());
    }

    @Test
    void mcpMessageWithoutCredentialsIsRejected() throws Exception {
        mvc.perform(post("/mcp/message")).andExpect(status().isUnauthorized());
    }

    @Test
    void sseWithWrongCredentialsIsRejected() throws Exception {
        mvc.perform(get("/sse").with(httpBasic("mcp", "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test
    void sseWithMcpCredentialsPassesSecurity() throws Exception {
        mvc.perform(get("/sse").with(httpBasic("mcp", "mcp-secret"))).andExpect(status().isNotFound());
    }

    @Test
    void sseWithAdminCredentialsAlsoPassesSecurity() throws Exception {
        mvc.perform(get("/sse").with(httpBasic("admin", "admin-secret"))).andExpect(status().isNotFound());
    }

    @Test
    void wrongUsernamePasswordPairingIsRejected() throws Exception {
        // "admin" the username, "mcp-secret" the password - not a valid pair for either account.
        mvc.perform(get("/sse").with(httpBasic("admin", "mcp-secret"))).andExpect(status().isUnauthorized());
    }

    @Test
    void mcpAccountIsAuthenticatedButNotAuthorizedForAdminOnlyEndpoints() throws Exception {
        // Valid login, wrong role: 403 (authenticated, forbidden), not 401 (no/bad credentials).
        mvc.perform(post("/project/save-project").with(httpBasic("mcp", "mcp-secret")))
                .andExpect(status().isForbidden());
    }
}
