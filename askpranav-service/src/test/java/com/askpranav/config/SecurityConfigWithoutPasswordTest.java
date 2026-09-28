package com.askpranav.config;

import com.askpranav.controller.ProjectController;
import com.askpranav.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With no ASKPRANAV_ADMIN_PASSWORD configured there must be no way in at all - no default login. */
@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
class SecurityConfigWithoutPasswordTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void noPasswordConfiguredMeansNoAccountExists() throws Exception {
        for (String password : new String[] {"", "admin", "password", "changeme"}) {
            mvc.perform(post("/project/save-project").with(httpBasic("admin", password))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(projectService);
    }
}
