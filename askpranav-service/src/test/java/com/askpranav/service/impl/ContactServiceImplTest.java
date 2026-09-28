package com.askpranav.service.impl;

import com.askpranav.dto.ContactRequest;
import com.askpranav.service.ContactUnavailableException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ContactServiceImplTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void sendsToOwnerWithVisitorAsReplyTo() {
        ContactServiceImpl service = new ContactServiceImpl(mailSender, "owner@example.com", "site@example.com");

        service.sendMessage(new ContactRequest("Ada", " ada@example.com ", " Hello there ", null));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mail = captor.getValue();
        assertThat(mail.getTo()).containsExactly("owner@example.com");
        assertThat(mail.getFrom()).isEqualTo("site@example.com");
        assertThat(mail.getReplyTo()).isEqualTo("ada@example.com");
        assertThat(mail.getSubject()).isEqualTo("[AskPranav] Message from Ada");
        assertThat(mail.getText()).contains("Ada <ada@example.com>").contains("Hello there");
    }

    @Test
    void lineBreaksInTheNameCannotInjectHeaders() {
        ContactServiceImpl service = new ContactServiceImpl(mailSender, "owner@example.com", "");

        service.sendMessage(new ContactRequest("Eve\r\nBcc: victim@example.com", "eve@example.com", "Hi", null));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).doesNotContain("\r").doesNotContain("\n");
    }

    @Test
    void refusesToSendWhenNoRecipientIsConfigured() {
        ContactServiceImpl service = new ContactServiceImpl(mailSender, "", "");

        assertThatThrownBy(() -> service.sendMessage(new ContactRequest("Ada", "ada@example.com", "Hi", null)))
                .isInstanceOf(ContactUnavailableException.class);

        verifyNoInteractions(mailSender);
    }

    @Test
    void mailServerFailureBecomesContactUnavailable() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        ContactServiceImpl service = new ContactServiceImpl(mailSender, "owner@example.com", "");

        assertThatThrownBy(() -> service.sendMessage(new ContactRequest("Ada", "ada@example.com", "Hi", null)))
                .isInstanceOf(ContactUnavailableException.class);
    }
}
