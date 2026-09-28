package com.askpranav.controller;

import com.askpranav.ai.web.AskRateLimiter;
import com.askpranav.dto.ContactRequest;
import com.askpranav.dto.ContactResponse;
import com.askpranav.service.ContactService;
import com.askpranav.service.ContactUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint behind the site's "Contact Me" form. Because anyone can call it and each call sends an
 * email, it is limited per client IP (default 3 messages per 10 minutes) and has a honeypot field.
 */
@CrossOrigin(origins = "${askpranav.cors.allowed-origin:http://localhost:4200}", allowedHeaders = "*")
@RestController
@RequestMapping("/contact")
@Slf4j
public class ContactController {

    private final ContactService contactService;
    private final AskRateLimiter rateLimiter;

    public ContactController(ContactService contactService,
                             @Value("${askpranav.contact.rate-limit.max-requests:3}") int maxRequests,
                             @Value("${askpranav.contact.rate-limit.window-seconds:600}") long windowSeconds) {
        this.contactService = contactService;
        this.rateLimiter = new AskRateLimiter(maxRequests, windowSeconds);
    }

    @PostMapping("/send")
    public ResponseEntity<ContactResponse> send(@Valid @RequestBody ContactRequest request, HttpServletRequest http) {
        AskRateLimiter.Decision decision = rateLimiter.tryAcquire(http.getRemoteAddr());
        if (!decision.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()))
                    .body(new ContactResponse("You have sent several messages recently. Please try again in "
                            + Math.max(1, (decision.retryAfterSeconds() + 59) / 60) + " minute(s)."));
        }
        if (request.website() != null && !request.website().isBlank()) {
            log.info("Contact form honeypot filled; message dropped");
            return ResponseEntity.ok(new ContactResponse("Thank you! Your message has been sent."));
        }
        contactService.sendMessage(request);
        return ResponseEntity.ok(new ContactResponse("Thank you! Your message has been sent."));
    }

    @ExceptionHandler(ContactUnavailableException.class)
    public ResponseEntity<ContactResponse> unavailable(ContactUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ContactResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ContactResponse> invalid(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(new ContactResponse("Please enter your name, a valid email address and a message."));
    }
}
