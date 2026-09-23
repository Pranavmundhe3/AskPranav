package com.askpranav.service.impl;

import com.askpranav.domain.Publication;
import com.askpranav.repository.PublicationRepository;
import com.askpranav.service.PublicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PublicationServiceImpl implements PublicationService {

    @Autowired
    private PublicationRepository publicationRepository;

    @Override
    public List<Publication> getPublicationDetails() {
        return publicationRepository.findAllByOrderByTitleAsc();
    }

    @Override
    public Optional<Publication> getPublicationByTitle(String title) {
        return publicationRepository.findFirstByTitleContainingIgnoreCase(title);
    }

    @Override
    public Publication savePublication(Publication publication) {
        return publicationRepository.save(publication);
    }
}
