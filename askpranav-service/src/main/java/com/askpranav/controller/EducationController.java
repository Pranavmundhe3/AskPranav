package com.askpranav.controller;

import com.askpranav.domain.Education;
import com.askpranav.service.EducationService;
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
@RequestMapping("/education")
@Slf4j
public class EducationController {

    @Autowired
    private EducationService educationService;

    @GetMapping(value = "/education-details")
    public ResponseEntity<List<Education>> getEducationDetails() {
        log.info("Get Education details Controller");
        return new ResponseEntity<>(educationService.getEducationDetails(), HttpStatus.OK);
    }

    @PostMapping(value = "/save-education")
    public ResponseEntity<Education> saveEducation(@RequestBody Education education) {
        return new ResponseEntity<>(educationService.saveEducation(education), HttpStatus.CREATED);
    }
}
