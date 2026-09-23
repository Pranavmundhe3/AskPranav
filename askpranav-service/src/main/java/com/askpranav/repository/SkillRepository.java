package com.askpranav.repository;

import com.askpranav.domain.Skills;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skills, Long> {

    List<Skills> findByTypeContainingIgnoreCase(String type);
}
