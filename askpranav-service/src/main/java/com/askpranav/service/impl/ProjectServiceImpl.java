package com.askpranav.service.impl;

import com.askpranav.domain.Project;
import com.askpranav.repository.ProjectRepository;
import com.askpranav.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public List<Project> getProjectDetails() {
        return projectRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Optional<Project> getProjectByName(String name) {
        return projectRepository.findFirstByNameContainingIgnoreCase(name);
    }

    @Override
    public Project saveProject(Project project) {
        return projectRepository.save(project);
    }
}
