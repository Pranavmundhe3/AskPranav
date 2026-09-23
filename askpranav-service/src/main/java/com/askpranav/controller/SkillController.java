package com.askpranav.controller;

import com.askpranav.domain.Skills;
import com.askpranav.service.SkillService;
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
@RequestMapping("/skill")
@Slf4j
public class SkillController {

    @Autowired
    private SkillService skillService;

    @GetMapping(value = "/skill-details")
    public ResponseEntity<List<Skills>> getSkillDetails() {
        log.info("Get Skill details Controller");
        return new ResponseEntity<>(skillService.getSkillDetails(), HttpStatus.OK);
    }

    @GetMapping(value = "/skill-details/by-type")
    public ResponseEntity<List<Skills>> getSkillsByType(@RequestParam String type) {
        return new ResponseEntity<>(skillService.getSkillsByType(type), HttpStatus.OK);
    }

    @PostMapping(value = "/save-skill")
    public ResponseEntity<Skills> saveSkill(@RequestBody Skills skill) {
        return new ResponseEntity<>(skillService.saveSkill(skill), HttpStatus.CREATED);
    }
}
