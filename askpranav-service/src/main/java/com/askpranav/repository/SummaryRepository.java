package com.askpranav.repository;

import com.askpranav.domain.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
}
