package com.askpranav.controller;

import com.askpranav.domain.Project;
import com.askpranav.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "${askpranav.cors.allowed-origin:http://localhost:4200}", allowedHeaders = "*")
@RestController
@RequestMapping("/project")
@Slf4j
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @GetMapping(value = "/project-details")
    public ResponseEntity<List<Project>> getProjectDetails() {
        log.info("Get Project details Controller");
        return new ResponseEntity<>(projectService.getProjectDetails(), HttpStatus.OK);
    }

    @PostMapping(value = "/save-project")
    public ResponseEntity<Project> saveProject(@RequestBody Project project) {
        return new ResponseEntity<>(projectService.saveProject(project), HttpStatus.CREATED);
    }
}
