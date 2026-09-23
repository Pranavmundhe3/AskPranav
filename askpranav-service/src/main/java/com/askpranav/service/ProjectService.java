package com.askpranav.service;

import com.askpranav.domain.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectService {

    List<Project> getProjectDetails();

    Optional<Project> getProjectByName(String name);

    Project saveProject(Project project);
}
