package com.askpranav.controller;

import com.askpranav.config.SecurityConfig;
import com.askpranav.dto.ContactRequest;
import com.askpranav.service.ContactService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "askpranav.contact.rate-limit.max-requests=3",
        "askpranav.contact.rate-limit.window-seconds=600"
})
class ContactControllerRateLimitTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ContactService contactService;

    @Test
    void fourthMessageInTheWindowIsRateLimited() throws Exception {
        String json = """
                {"name":"Recruiter","email":"recruiter@example.com","message":"Hello"}""";
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/contact/send").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isOk());
        }

        mvc.perform(post("/contact/send").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        verify(contactService, times(3)).sendMessage(any(ContactRequest.class));
    }
}
