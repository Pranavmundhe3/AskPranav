package com.askpranav.repository;

import com.askpranav.domain.Certifications;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certifications, Long> {
}
