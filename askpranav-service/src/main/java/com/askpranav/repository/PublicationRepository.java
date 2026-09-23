package com.askpranav.repository;

import com.askpranav.domain.Publication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    Optional<Publication> findFirstByTitleContainingIgnoreCase(String title);

    List<Publication> findAllByOrderByTitleAsc();
}
