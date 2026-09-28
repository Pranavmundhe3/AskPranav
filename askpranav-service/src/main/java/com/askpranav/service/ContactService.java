package com.askpranav.service;

import com.askpranav.dto.ContactRequest;

public interface ContactService {

    /**
     * Emails the visitor's message to the site owner.
     *
     * @throws ContactUnavailableException if mail is not configured or could not be sent
     */
    void sendMessage(ContactRequest request);
}
