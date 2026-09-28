package com.askpranav.service.impl;

import com.askpranav.dto.ContactRequest;
import com.askpranav.service.ContactService;
import com.askpranav.service.ContactUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ContactServiceImpl implements ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String to;
    private final String from;

    public ContactServiceImpl(JavaMailSender mailSender,
                              @Value("${askpranav.contact.to:}") String to,
                              @Value("${askpranav.contact.from:}") String from) {
        this.mailSender = mailSender;
        this.to = to;
        this.from = from;
    }

    @Override
    public void sendMessage(ContactRequest request) {
        if (to == null || to.isBlank()) {
            throw new ContactUnavailableException("The contact form is not configured yet.");
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        if (from != null && !from.isBlank()) {
            // Most SMTP providers only accept the authenticated account as sender, so the visitor's
            // address goes in Reply-To instead: hitting "reply" answers them directly.
            mail.setFrom(from);
        }
        mail.setReplyTo(request.email().trim());
        mail.setSubject("[AskPranav] Message from " + singleLine(request.name()));
        mail.setText("From: " + singleLine(request.name()) + " <" + request.email().trim() + ">\n\n"
                + request.message().trim());

        try {
            mailSender.send(mail);
            log.info("Contact message forwarded by email");
        } catch (MailException e) {
            log.warn("Could not send contact message: {}", e.getMessage());
            throw new ContactUnavailableException("The message could not be sent right now.", e);
        }
    }

    /** Header values must not contain line breaks, or a visitor could inject extra mail headers. */
    private static String singleLine(String value) {
        return value.replaceAll("[\\r\\n]+", " ").trim();
    }
}
