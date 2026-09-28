package com.askpranav.controller;

import com.askpranav.config.SecurityConfig;
import com.askpranav.dto.ContactRequest;
import com.askpranav.service.ContactService;
import com.askpranav.service.ContactUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The limiter lives in the (cached) controller bean, so give these tests headroom; the limit itself is
// covered in ContactControllerRateLimitTest.
@WebMvcTest(ContactController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "askpranav.contact.rate-limit.max-requests=1000")
class ContactControllerTest {

    private static final String VALID = """
            {"name":"Recruiter","email":"recruiter@example.com","message":"Let's talk about a role."}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ContactService contactService;

    private org.springframework.test.web.servlet.ResultActions send(String json) throws Exception {
        return mvc.perform(post("/contact/send").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void validMessageIsForwardedWithoutLogin() throws Exception {
        send(VALID).andExpect(status().isOk()).andExpect(jsonPath("$.message").exists());

        verify(contactService).sendMessage(any(ContactRequest.class));
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        send("""
                {"name":"Recruiter","email":"not-an-email","message":"Hi"}""")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
    }

    @Test
    void blankMessageIsRejected() throws Exception {
        send("""
                {"name":"Recruiter","email":"recruiter@example.com","message":"  "}""")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contactService);
    }

    @Test
    void filledHoneypotLooksSuccessfulButSendsNothing() throws Exception {
        send("""
                {"name":"Bot","email":"bot@example.com","message":"buy now","website":"http://spam.example"}""")
                .andExpect(status().isOk());

        verifyNoInteractions(contactService);
    }

    @Test
    void mailProblemsSurfaceAs503() throws Exception {
        doThrow(new ContactUnavailableException("The contact form is not configured yet."))
                .when(contactService).sendMessage(any(ContactRequest.class));

        send(VALID).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("The contact form is not configured yet."));
    }

}
