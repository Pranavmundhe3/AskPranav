package com.askpranav.config;

import com.askpranav.controller.ProjectController;
import com.askpranav.domain.Project;
import com.askpranav.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer test of the write protection; no database or model involved. */
@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "askpranav.security.admin-username=admin",
        "askpranav.security.admin-password=test-secret"
})
class SecurityConfigTest {

    private static final String SAVE = "/project/save-project";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void readsArePublic() throws Exception {
        when(projectService.getProjectDetails()).thenReturn(List.of());

        mvc.perform(get("/project/project-details")).andExpect(status().isOk());
    }

    @Test
    void writeWithoutCredentialsIsRejectedAndNothingIsSaved() throws Exception {
        mvc.perform(post(SAVE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(projectService);
    }

    @Test
    void writeWithWrongPasswordIsRejected() throws Exception {
        mvc.perform(post(SAVE).with(httpBasic("admin", "wrong")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(projectService);
    }

    @Test
    void writeWithAdminCredentialsIsAllowed() throws Exception {
        when(projectService.saveProject(any(Project.class))).thenReturn(new Project());

        mvc.perform(post(SAVE).with(httpBasic("admin", "test-secret")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());

        verify(projectService).saveProject(any(Project.class));
    }

    @Test
    void anyOtherWriteMethodIsDeniedByDefault() throws Exception {
        mvc.perform(delete("/project/anything")).andExpect(status().isUnauthorized());
    }

    @Test
    void browserPreflightForAWriteIsAnsweredWithoutCredentials() throws Exception {
        mvc.perform(options(SAVE)
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
}
