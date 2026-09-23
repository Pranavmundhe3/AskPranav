package com.askpranav.service;

import com.askpranav.domain.Publication;

import java.util.List;
import java.util.Optional;

public interface PublicationService {

    List<Publication> getPublicationDetails();

    Optional<Publication> getPublicationByTitle(String title);

    Publication savePublication(Publication publication);
}
