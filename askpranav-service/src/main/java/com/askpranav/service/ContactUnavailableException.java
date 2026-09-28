package com.askpranav.service;

/** The contact form cannot deliver right now (not configured, or the mail server refused). */
public class ContactUnavailableException extends RuntimeException {

    public ContactUnavailableException(String message) {
        super(message);
    }

    public ContactUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
