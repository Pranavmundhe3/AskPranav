package com.askpranav.controller;

import com.askpranav.domain.Experience;
import com.askpranav.service.ExperienceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "${askpranav.cors.allowed-origin:http://localhost:4200}", allowedHeaders = "*")
@RestController
@RequestMapping("/experience")
@Slf4j
public class ExperienceController {

    @Autowired
    private ExperienceService experienceService;

    @GetMapping(value = "/experience-details")
    public ResponseEntity<List<Experience>> getExperienceDetails() {
        log.info("Get Experience details Controller");
        return new ResponseEntity<>(experienceService.getExperienceDetails(), HttpStatus.OK);
    }

    @GetMapping(value = "/experience-details/by-company")
    public ResponseEntity<List<Experience>> getExperienceByCompany(@RequestParam String company) {
        return new ResponseEntity<>(experienceService.getExperienceByCompany(company), HttpStatus.OK);
    }

    @PostMapping(value = "/save-experience")
    public ResponseEntity<Experience> saveExperience(@RequestBody Experience experience) {
        return new ResponseEntity<>(experienceService.saveExperience(experience), HttpStatus.CREATED);
    }
}
