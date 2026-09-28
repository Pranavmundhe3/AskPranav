package com.askpranav.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A message from the site's contact form. {@code website} is a honeypot: it is hidden from real visitors,
 * so anything in it means an automated form-filler, and the message is dropped silently.
 */
public record ContactRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(max = 5000) String message,
        @Size(max = 200) String website
) {
}
