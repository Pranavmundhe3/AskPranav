package com.askpranav.repository;

import com.askpranav.domain.Experience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    List<Experience> findByCompanyContainingIgnoreCase(String company);
}
