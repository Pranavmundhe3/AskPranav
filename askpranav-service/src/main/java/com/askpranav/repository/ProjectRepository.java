package com.askpranav.repository;

import com.askpranav.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findFirstByNameContainingIgnoreCase(String name);

    List<Project> findAllByOrderByNameAsc();
}
